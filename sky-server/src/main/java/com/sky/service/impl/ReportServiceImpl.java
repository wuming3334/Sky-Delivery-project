package com.sky.service.impl;

import com.sky.dto.DateGroupDTO;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
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
    @Autowired
    private WorkspaceService workspaceService;
    /**
     * 营业额统计（批量查询）
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
     * 用户统计（批量查询）
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
     * 订单统计批量查询
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        // 生成日期列表
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
        List<DateGroupDTO> validOrderCountData = orderMapper. countCompletedOrderGroupByDate(beginTime, endTime, Orders.COMPLETED);

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
        // 结束日期加一天确保包含end当天所有数据
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
    /**
     * 导出运营数据报表
     * @param response
     */
    public void exportBusinessData(HttpServletResponse response) {
        //1. 查询数据库，获取营业数据---查询最近30天的运营数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        //查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));
        //按日期分组批量查询所有数据 提升性能
        List<BusinessDataVO> businessDataVOs = workspaceService.GetBusinessDataList(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));

        //2. 通过POI将数据写入到Excel文件中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            //基于模板文件创建一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);

            //获取表格文件的Sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");

            //填充数据--时间
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);

            //获得第4行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            //获得第5行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());
            //

            //填充明细数据
          /*  for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                //查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                //获得某一行
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }*/
            for (int i = 0; i < businessDataVOs.size(); i++) {
                LocalDate date = dateBegin.plusDays(i);
                BusinessDataVO tempBusinessDataVO = businessDataVOs.get(i);
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(tempBusinessDataVO.getTurnover());
                row.getCell(3).setCellValue(tempBusinessDataVO.getValidOrderCount());
                row.getCell(4).setCellValue(tempBusinessDataVO.getOrderCompletionRate());
                row.getCell(5).setCellValue(tempBusinessDataVO.getUnitPrice());
                row.getCell(6).setCellValue(tempBusinessDataVO.getNewUsers());
            }

            //3. 通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //关闭资源
            out.close();
            excel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
