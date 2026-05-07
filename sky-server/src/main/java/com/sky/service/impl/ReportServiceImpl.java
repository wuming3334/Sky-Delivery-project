package com.sky.service.impl;

import com.sky.dto.DateGroupDTO;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 营业额统计（优化版：批量查询）
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 生成日期列表（确保包含今天）
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate actualEnd = end.isBefore(today) ? today : end;
        
        LocalDate current = begin;
        dateList.add(current);
        while (!current.equals(actualEnd)) {
            current = current.plusDays(1);
            dateList.add(current);
        }

        // 批量查询：一次SQL获取所有日期的营业额
        LocalDateTime beginTime = LocalDateTime.of(dateList.get(0), LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(actualEnd.plusDays(1), LocalTime.MIN);
        List<DateGroupDTO> turnoverData = orderMapper.sumAmountGroupByDate(beginTime, endTime, Orders.COMPLETED);

        // 将查询结果转为Map，方便查找
        Map<String, Double> turnoverMap = turnoverData.stream()
                .collect(Collectors.toMap(
                        DateGroupDTO::getDate,
                        dto -> dto.getValue() != null ? dto.getValue().doubleValue() : 0.0
                ));

        // 按日期顺序填充数据（没有数据的日期填0）
        List<Double> turnoverList = dateList.stream()
                .map(date -> turnoverMap.getOrDefault(date.toString(), 0.0))
                .collect(Collectors.toList());

        return TurnoverReportVO.builder()
                .dateList(String.join(",", dateList.stream().map(String::valueOf).collect(Collectors.toList())))
                .turnoverList(String.join(",", turnoverList.stream().map(String::valueOf).collect(Collectors.toList())))
                .build();
    }

    /**
     * 用户统计（优化版：批量查询）
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 生成日期列表（确保包含今天）
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate actualEnd = end.isBefore(today) ? today : end;
        
        LocalDate current = begin;
        dateList.add(current);
        while (!current.equals(actualEnd)) {
            current = current.plusDays(1);
            dateList.add(current);
        }

        // 批量查询：一次SQL获取所有日期的新增用户数
        LocalDateTime beginTime = LocalDateTime.of(dateList.get(0), LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(actualEnd.plusDays(1), LocalTime.MIN);
        List<DateGroupDTO> newUserListData = userMapper.countNewUserGroupByDate(beginTime, endTime);

        // 将查询结果转为Map
        Map<String, Integer> newUserMap = newUserListData.stream()
                .collect(Collectors.toMap(
                        DateGroupDTO::getDate,
                        dto -> dto.getValue() != null ? dto.getValue().intValue() : 0
                ));

        // 按日期顺序填充数据
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            // 当日新增用户
            Integer newUserCount = newUserMap.getOrDefault(date.toString(), 0);
            newUserList.add(newUserCount);

            // 总用户数：截至当天结束的累计注册用户数
            LocalDateTime dayEndTime = LocalDateTime.of(date.plusDays(1), LocalTime.MIN);
            Integer totalUserCount = userMapper.countByTime(LocalDateTime.of(2000, 1, 1, 0, 0, 0), dayEndTime);
            totalUserList.add(totalUserCount != null ? totalUserCount : 0);
        }

        return UserReportVO.builder()
                .dateList(String.join(",", dateList.stream().map(String::valueOf).collect(Collectors.toList())))
                .newUserList(String.join(",", newUserList.stream().map(String::valueOf).collect(Collectors.toList())))
                .totalUserList(String.join(",", totalUserList.stream().map(String::valueOf).collect(Collectors.toList())))
                .build();
    }

    /**
     * 订单统计（优化版：批量查询）
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        // 生成日期列表（确保包含今天）
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate actualEnd = end.isBefore(today) ? today : end;
        
        LocalDate current = begin;
        dateList.add(current);
        while (!current.equals(actualEnd)) {
            current = current.plusDays(1);
            dateList.add(current);
        }

        // 批量查询：一次SQL获取所有日期的订单总数
        LocalDateTime beginTime = LocalDateTime.of(dateList.get(0), LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(actualEnd.plusDays(1), LocalTime.MIN);
        List<DateGroupDTO> orderCountData = orderMapper.countOrderGroupByDate(beginTime, endTime);

        // 批量查询：一次SQL获取所有日期的有效订单数
        List<Integer> validStatusList = Arrays.asList(Orders.COMPLETED);
        List<DateGroupDTO> validOrderCountData = orderMapper.countValidOrderGroupByDate(beginTime, endTime, validStatusList);

        // 将查询结果转为Map
        Map<String, Integer> orderCountMap = orderCountData.stream()
                .collect(Collectors.toMap(
                        DateGroupDTO::getDate,
                        dto -> dto.getValue() != null ? dto.getValue().intValue() : 0
                ));

        Map<String, Integer> validOrderCountMap = validOrderCountData.stream()
                .collect(Collectors.toMap(
                        DateGroupDTO::getDate,
                        dto -> dto.getValue() != null ? dto.getValue().intValue() : 0
                ));

        // 按日期顺序填充数据
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        int totalOrderCount = 0;
        int validOrderCount = 0;

        for (LocalDate date : dateList) {
            Integer orderCount = orderCountMap.getOrDefault(date.toString(), 0);
            orderCountList.add(orderCount);
            totalOrderCount += orderCount;

            Integer validCount = validOrderCountMap.getOrDefault(date.toString(), 0);
            validOrderCountList.add(validCount);
            validOrderCount += validCount;
        }

        // 订单完成率
        Double orderCompletionRate = totalOrderCount == 0 ? 0.0 : (double) validOrderCount / totalOrderCount;

        return OrderReportVO.builder()
                .dateList(String.join(",", dateList.stream().map(String::valueOf).collect(Collectors.toList())))
                .orderCountList(String.join(",", orderCountList.stream().map(String::valueOf).collect(Collectors.toList())))
                .validOrderCountList(String.join(",", validOrderCountList.stream().map(String::valueOf).collect(Collectors.toList())))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 销量排行top10
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        // LocalDate转LocalDateTime
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        // 结束日期加一天，确保包含end当天所有数据
        LocalDateTime endTime = LocalDateTime.of(end.plusDays(1), LocalTime.MIN);

        log.info("销量排行查询时间范围：{} ~ {}", beginTime, endTime);

        // 只统计已完成的订单
        List<Integer> validStatusList = Arrays.asList(Orders.COMPLETED);
        List<GoodsSalesDTO> salesTop10 = orderDetailMapper.getSalesTop10(beginTime, endTime, validStatusList);

        log.info("销量排行查询结果数量：{}", salesTop10.size());

        // 处理null值，转为字符串列表
        String nameList = String.join(",", 
                salesTop10.stream()
                        .map(GoodsSalesDTO::getName)
                        .collect(Collectors.toList()));

        // MySQL返回的number是BigDecimal类型，需要转换为Integer或Double
        String numberList = String.join(",", 
                salesTop10.stream()
                        .map(dto -> dto.getNumber().toString())
                        .collect(Collectors.toList()));

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }
}
