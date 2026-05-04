package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.*;
import com.sky.service.OrderService;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.sky.constant.MessageConstant.ADDRESS_BOOK_IS_NULL;
import static com.sky.constant.MessageConstant.SHOPPING_CART_IS_NULL;

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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //校验业务异常 购物车是否为空 地址是否为空
        AddressBook address = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (address == null) {
            throw new RuntimeException(ADDRESS_BOOK_IS_NULL);
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
}
