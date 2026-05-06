package com.sky.controller.user;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("user/order")
@Api(tags = "用户端订单接口")
public class OrderController {
    @Autowired
    private OrderService orderService;

    /**
     * 用户下单
     *
     * @param OrdersSubmitDTO
     * @return
     */
    @PostMapping("submit")
    @ApiOperation("用户下单接口")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO OrdersSubmitDTO) {
        OrderSubmitVO data = orderService.submit(OrdersSubmitDTO);
        return Result.success(data);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        orderService.paySuccess(ordersPaymentDTO.getOrderNumber());
        return Result.success(orderPaymentVO);
    }

    /**
     * 再来一单
     */
    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单")
    public Result reOrder( @PathVariable Long id) {
        orderService.reOrder(id);
        return Result.success();
    }
    /**
     * 查询订单详情
     */
    @GetMapping("/orderDetail/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> orderDetail(@PathVariable Long id) {
        OrderVO orderVO = orderService.getOrderDetail(id);
        return Result.success(orderVO);
    }
    /**
     * 取消订单
     */
    @PutMapping("/cancel/{id}")
    @ApiOperation("取消订单")
    public Result cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return Result.success();
    }

    /**
     * 查询历史订单
     */
    @GetMapping("/historyOrders")
    @ApiOperation("查询历史订单")
    public Result<PageResult> historyOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageResult pageResult = orderService.pageQuery4User(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 用户催单
     */
    @GetMapping("/reminder/{id}")
    @ApiOperation("用户催单")
    public Result reminder(@PathVariable Long id) {
        orderService.reminder(id);
        return Result.success();
    }

    /**
     * 商户接单
     */
    @PutMapping("/confirm")
    @ApiOperation("商户接单")
    public Result accept(Long  id) {
        orderService.confirm(id);
        return Result.success();
    }


}
