package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;
    @Scheduled(cron = "0 * * * * ? ")//每分钟执行一次
    public void processTimeoutOrder() {
        log.info("处理订单超时{}", LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        List<Orders> list = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);

        if (list != null && !list.isEmpty()){
            for (Orders orders : list) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时，自动取消");
                orders.setCancelTime(LocalDateTime.now());
            }
            orderMapper.batchCancelTimeoutOrders(list);
        }

    }
    @Scheduled(cron = "0 0 1 * * ? ")//每日凌晨1点执行一次
/*    @Scheduled(cron = "0/5 * * * * ? ")*/
    public void processDeliveryOrder() {
        log.info("处理订单状态为派送中的订单{}", LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        List<Orders> list = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, time);
        if (list != null && !list.isEmpty()){
            for (Orders orders : list) {
                orders.setStatus(Orders.COMPLETED);
            }
            orderMapper.batchCancelTimeoutOrders(list);
        }

    }
}
