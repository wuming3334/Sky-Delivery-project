package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.dto.DateGroupDTO;
import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 根据时间段统计营业数据
     * @param begin
     * @param end
     * @return
     */
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
        /**
         * 营业额：当日已完成订单的总金额
         * 有效订单：当日已完成订单的数量
         * 订单完成率：有效订单数 / 总订单数
         * 平均客单价：营业额 / 有效订单数
         * 新增用户：当日新增用户的数量
         */

        Map map = new HashMap();
        map.put("begin",begin);
        map.put("end",end);

        //查询总订单数
        Integer totalOrderCount = orderMapper.countByMap(map);

        map.put("status", Orders.COMPLETED);
        //营业额
        Double turnover = orderMapper.sumByTimeAndStatus(begin,end, Orders.COMPLETED);
        turnover = turnover == null? 0.0 : turnover;

        //有效订单数
        Integer validOrderCount = orderMapper.countByMap(map);

        Double unitPrice = 0.0;

        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0 && validOrderCount != 0){
            //订单完成率
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
            //平均客单价
            unitPrice = turnover / validOrderCount;
        }

        //新增用户数
        Integer newUsers = userMapper.countByTime(begin, end);

        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }
    // ... existing code ...
    /**
     * 根据时间段统计营业数据返回集合
     */

    /**
     * 根据时间段统计营业数据（返回列表）
     */
    public List<BusinessDataVO> GetBusinessDataList(LocalDateTime begin, LocalDateTime end) {
        //一次性查出所有原始数据
        List<DateGroupDTO> orders = orderMapper.countOrderGroupByDate(begin, end);
        List<DateGroupDTO> turnovers = orderMapper.sumAmountGroupByDate(begin, end, Orders.COMPLETED);
        List<DateGroupDTO> validOrders = orderMapper.countCompletedOrderGroupByDate(begin, end, Orders.COMPLETED);
        List<DateGroupDTO> newUsers = userMapper.countNewUserGroupByDate(begin, end);

        //转为 Map
        Map<String, Integer> orderMap = orders.stream().collect(Collectors.toMap(DateGroupDTO::getDate, dto -> dto.getValue().intValue()));
        Map<String, Double> turnoverMap = turnovers.stream().collect(Collectors.toMap(DateGroupDTO::getDate, dto -> dto.getValue().doubleValue()));
        Map<String, Integer> validOrderMap = validOrders.stream().collect(Collectors.toMap(DateGroupDTO::getDate, dto -> dto.getValue().intValue()));
        Map<String, Integer> newUserMap = newUsers.stream().collect(Collectors.toMap(DateGroupDTO::getDate, dto -> dto.getValue().intValue()));

        // 3. 循环组装
        List<BusinessDataVO> result = new ArrayList<>();
        LocalDate curr = begin.toLocalDate();
        while (!curr.isAfter(end.toLocalDate())) {
            String key = curr.toString();

            Integer totalOrders = orderMap.getOrDefault(key, 0);
            Double turnover = turnoverMap.getOrDefault(key, 0.0);
            Integer validOrder = validOrderMap.getOrDefault(key, 0);
            Integer newUser = newUserMap.getOrDefault(key, 0);

            double completionRate = totalOrders == 0 ? 0.0 : (double) validOrder / totalOrders;
            double unitPrice = validOrder == 0 ? 0.0 : turnover / validOrder;

            BusinessDataVO vo = BusinessDataVO.builder()
                    .turnover(turnover)
                    .validOrderCount(validOrder)
                    .orderCompletionRate(completionRate)
                    .unitPrice(unitPrice)
                    .newUsers(newUser)
                    .build();

            result.add(vo);
            curr = curr.plusDays(1);
        }
        return result;
    }




    /**
     * 查询订单管理数据
     *
     * @return
     */
    public OrderOverViewVO getOrderOverView() {
        Map map = new HashMap();
        map.put("begin", LocalDateTime.now().with(LocalTime.MIN));
        map.put("status", Orders.TO_BE_CONFIRMED);

        //待接单
        Integer waitingOrders = orderMapper.countByMap(map);

        //待派送
        map.put("status", Orders.CONFIRMED);
        Integer deliveredOrders = orderMapper.countByMap(map);

        //已完成
        map.put("status", Orders.COMPLETED);
        Integer completedOrders = orderMapper.countByMap(map);

        //已取消
        map.put("status", Orders.CANCELLED);
        Integer cancelledOrders = orderMapper.countByMap(map);

        //全部订单
        map.put("status", null);
        Integer allOrders = orderMapper.countByMap(map);

        return OrderOverViewVO.builder()
                .waitingOrders(waitingOrders)
                .deliveredOrders(deliveredOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .allOrders(allOrders)
                .build();
    }

    /**
     * 查询菜品总览
     *
     * @return
     */
    public DishOverViewVO getDishOverView() {
        Map map = new HashMap();
        map.put("status", StatusConstant.ENABLE);
        Integer sold = dishMapper.countByMap(map);

        map.put("status", StatusConstant.DISABLE);
        Integer discontinued = dishMapper.countByMap(map);

        return DishOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    /**
     * 查询套餐总览
     *
     * @return
     */
    public SetmealOverViewVO getSetmealOverView() {
        Map map = new HashMap();
        map.put("status", StatusConstant.ENABLE);
        Integer sold = setmealMapper.countByMap(map);

        map.put("status", StatusConstant.DISABLE);
        Integer discontinued = setmealMapper.countByMap(map);

        return SetmealOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }
}
