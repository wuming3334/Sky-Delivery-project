package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.JsonObject;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.DistanceUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.sky.constant.MessageConstant.ADDRESS_BOOK_IS_NULL;
import static com.sky.constant.MessageConstant.BEYOND_DELIVERY_RANGE;
import static com.sky.constant.MessageConstant.SHOPPING_CART_IS_NULL;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WebSocketServer webSocketServer;
    @Autowired
    private RedisTemplate redisTemplate;

    private static final String SHOP_ADDRESS = "北京市海淀区上地十街10号";
    private static final double MAX_DELIVERY_DISTANCE = 5000.0;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //校验业务异常 购物车是否为空 地址是否为空
        AddressBook address = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (address == null) {
            throw new RuntimeException(ADDRESS_BOOK_IS_NULL);
        }

        if (!DistanceUtil.isWithinRange(address.getDetail(), SHOP_ADDRESS, MAX_DELIVERY_DISTANCE)) {
            throw new OrderBusinessException(BEYOND_DELIVERY_RANGE);
        }
        //获取用户id 赋值给一个购物车实体
        Long userId = BaseContext.getCurrentId();
        //根据用户id查询用户购物车数据
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.list(userId);
        //获取购物车数据
        if (shoppingCarts == null || shoppingCarts.size() == 0) {
            throw new RuntimeException(SHOPPING_CART_IS_NULL);
        }

        //拷贝dto数据到order对象,再插入数据
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, order);

        //补全订单属性
        String orderNumber = UUID.randomUUID().toString().replace("-", "").substring(0, 18);
        order.setNumber(orderNumber);
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setUserId(userId);
        order.setOrderTime(LocalDateTime.now());
        order.setPayStatus(Orders.UN_PAID);
        order.setCheckoutTime(LocalDateTime.now());
        order.setPhone(address.getPhone());
        order.setConsignee(address.getConsignee());
        order.setAddress(address.getDetail());
        order.setUserName(address.getConsignee());
        BigDecimal amount = shoppingCarts.stream()
                .map(cart -> cart.getAmount().multiply(new BigDecimal(cart.getNumber())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setAmount(amount);
        orderMapper.insert(order);
        //计算订单金额


        //封装订单详情数据
        List<OrderDetail> orderDetails = shoppingCarts.stream().map(cart -> {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(order.getId());
            //封装冗余字段
            orderDetail.setName(cart.getName());
            orderDetail.setImage(cart.getImage());
            return orderDetail;
        }).collect(Collectors.toList());

        //插入订单详情数据
        if (orderDetails != null && !orderDetails.isEmpty()) {
            orderDetailMapper.insertBatch(orderDetails);
        }
        //删除购物车数据
        shoppingCartMapper.deleteByUserId(userId);

        //扣减菜品和套餐的库存todo
        //封装VO返回
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder().
                orderNumber(orderNumber).
                orderTime(order.getOrderTime()).
                orderAmount(order.getAmount()).
                id(order.getId()).build();

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
       /* JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );*/
        JSONObject jsonObject = new JSONObject();
        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
        //来单提醒
        pushOrderNotice(ordersDB, 1);
    }

    /**
     * 再来一单
     *
     * @param
     */
    @Override
    public void reOrder(Long orderId) {
        //遍历并复制订单详情的购物车数据
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);
        List<ShoppingCart> shoppingCarts = new ArrayList<>();
        for (OrderDetail orderDetail : orderDetails) {
            ShoppingCart shoppingCart = new ShoppingCart();
            shoppingCart.setName(orderDetail.getName());
            shoppingCart.setImage(orderDetail.getImage());
            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCart.setNumber(orderDetail.getNumber());
            shoppingCart.setAmount(orderDetail.getAmount());
            shoppingCart.setCreateTime(LocalDateTime.now());
            if (orderDetail.getDishId() != null) {
                shoppingCart.setDishId(orderDetail.getDishId());
                if (orderDetail.getDishFlavor() != null) {
                    shoppingCart.setDishFlavor(orderDetail.getDishFlavor());
                }
            } else {
                shoppingCart.setSetmealId(orderDetail.getSetmealId());
            }
            shoppingCarts.add(shoppingCart);
        }
        //批量插入购物车数据
        shoppingCartMapper.insertBatch(shoppingCarts);
    }

    /**
     * 查询订单详情
     *
     * @param id 订单id
     * @return
     */
    @Override
    public OrderVO getOrderDetail(Long id) {
        Orders orders = orderMapper.getById(id);

        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetails);

        return orderVO;
    }

    /**
     * 取消订单
     *
     * @param
     */
    @Override
    public void cancel(Long id) {
        Orders orders = orderMapper.getById(id);
        //校验异常
        if (orders == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!orders.getUserId().equals(BaseContext.getCurrentId())) {
            throw new RuntimeException("无权操作此订单");
        }

        if (orders.getStatus() > Orders.TO_BE_CONFIRMED) {
            throw new RuntimeException("订单不能取消");
        }
        //修改状态 并添加必要字段
        Orders updateOrders = Orders.builder()
                .id(id)
                .status(Orders.CANCELLED)
                .cancelReason("用户取消订单")
                .cancelTime(LocalDateTime.now())
                .build();

        orderMapper.update(updateOrders);
    }

    /**
     * 查询用户历史订单 分页
     *
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery4User(OrdersPageQueryDTO ordersPageQueryDTO) {
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());

        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        List<Orders> ordersList = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> orderVOList = new ArrayList<>();
        if (ordersList != null && !ordersList.isEmpty()) {
            for (Orders orders : ordersList) {
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders.getId());

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                orderVOList.add(orderVO);
            }
        }

        PageInfo<Orders> pageInfo = new PageInfo<>(ordersList);
        return new PageResult(pageInfo.getTotal(), orderVOList);
    }

    public void pushOrderNotice(Orders ordersDB, Integer status) {
        //调用websocket，推送来单提醒或者催单提醒
        //先定义一个map，里面存放要推送提醒数据
        Map map = new HashMap();
        map.put("type", status);//1表示来单提醒,2表示用户催单
        map.put("orderId", ordersDB.getId());//订单id
        map.put("content", "订单号:" + ordersDB.getNumber());//推送提醒内容
        //转为 json
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    @Override
    public void reminder(Long id) {
        Orders orders = orderMapper.getById(id);

        if (orders == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!orders.getUserId().equals(BaseContext.getCurrentId())) {
            throw new RuntimeException("无权操作此订单");
        }

        if (orders.getStatus() > Orders.TO_BE_CONFIRMED) {
            throw new RuntimeException("订单已接单，无需催单");
        }
        //使用redis实现订单催单功能
        Long userId = BaseContext.getCurrentId();
        String key = "order_reminder_" + userId + "_" + id;
        Boolean hasReminded = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(hasReminded)) {
            throw new RuntimeException("十分钟内只能催单一次");
        }

        redisTemplate.opsForValue().set(key, "1", 10, TimeUnit.MINUTES);

        pushOrderNotice(orders, 2);
    }

    /**
     * 接单
     *
     * @param id
     */
    @Override
    public void accept(Long id) {
        Orders orders = orderMapper.getById(id);

        if (orders == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!orders.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new RuntimeException("订单状态不是待接单，无法接单");
        }

        Orders updateOrders = Orders.builder()
                .id(id)
                .status(Orders.CONFIRMED)
                .build();

        orderMapper.update(updateOrders);
    }

    /**
     * 商家端订单分页查询
     *
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        List<Orders> ordersList = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> orderVOList = new ArrayList<>();
        if (ordersList != null && !ordersList.isEmpty()) {
            for (Orders orders : ordersList) {
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders.getId());

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                orderVOList.add(orderVO);
            }
        }

        PageInfo<Orders> pageInfo = new PageInfo<>(ordersList);
        return new PageResult(pageInfo.getTotal(), orderVOList);
    }

    /**
     * 各个状态订单数量统计
     *
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        // 待接单数量
        Integer toBeConfirmed = orderMapper.countByStatus(Orders.TO_BE_CONFIRMED);
        // 已接单（待派送）数量
        Integer confirmed = orderMapper.countByStatus(Orders.CONFIRMED);
        // 派送中数量
        Integer deliveryInProgress = orderMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS);

        return OrderStatisticsVO.builder()
                .toBeConfirmed(toBeConfirmed)
                .confirmed(confirmed)
                .deliveryInProgress(deliveryInProgress)
                .build();
    }

    /**
     * 查询订单详情
     *
     * @param id
     * @return
     */
    @Override
    public OrderVO details(Long id) {
        Orders orders = orderMapper.getById(id);

        if (orders == null) {
            throw new RuntimeException("订单不存在");
        }

        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetails);

        return orderVO;
    }

    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders orders = orderMapper.getById(ordersRejectionDTO.getId());

        if (orders == null) {
            throw new RuntimeException("订单不存在");
        }

        // 只有待接单状态的订单才能拒单
        if (!orders.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new RuntimeException("订单状态不是待接单，无法拒单");
        }

        Orders updateOrders = Orders.builder()
                .id(ordersRejectionDTO.getId())
                .status(Orders.CANCELLED)
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .build();

        orderMapper.update(updateOrders);
    }

    /**
     * 商家取消订单
     *
     * @param ordersCancelDTO
     */
    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        Orders orders = orderMapper.getById(ordersCancelDTO.getId());

        if (orders == null) {
            throw new RuntimeException("订单不存在");
        }

        // 只有已接单状态的订单才能由商家取消
        if (!orders.getStatus().equals(Orders.CONFIRMED)) {
            throw new RuntimeException("订单状态不是已接单，无法取消");
        }

        Orders updateOrders = Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();

        orderMapper.update(updateOrders);
    }

    /**
     * 派送订单
     *
     * @param id
     */
    @Override
    public void delivery(Long id) {
        Orders orders = orderMapper.getById(id);

        if (orders == null) {
            throw new RuntimeException("订单不存在");
        }

        // 只有已接单状态的订单才能派送
        if (!orders.getStatus().equals(Orders.CONFIRMED)) {
            throw new RuntimeException("订单状态不是已接单，无法派送");
        }

        Orders updateOrders = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();

        orderMapper.update(updateOrders);
    }

    /**
     * 完成订单
     *
     * @param id
     */
    @Override
    public void complete(Long id) {
        Orders orders = orderMapper.getById(id);

        if (orders == null) {
            throw new RuntimeException("订单不存在");
        }

        // 只有派送中状态的订单才能完成
        if (!orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new RuntimeException("订单状态不是派送中，无法完成");
        }

        Orders updateOrders = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();

        orderMapper.update(updateOrders);
    }


}

