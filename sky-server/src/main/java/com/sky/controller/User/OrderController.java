package com.sky.controller.user;

import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("user/order")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @PostMapping("submit")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO OrdersSubmitDTO){
        OrderSubmitVO data = orderService.submit(OrdersSubmitDTO);
        return Result.success(data);
    }
}
