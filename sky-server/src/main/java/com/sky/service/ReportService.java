package com.sky.service;

import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

public interface ReportService {

    /**
     * 营业额统计
     * @param begin 开始日期
     * @param end 结束日期
     * @return
     */
    TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end);

    /**
     * 用户统计
     * @param begin 开始日期
     * @param end 结束日期
     * @return
     */
    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);

    /**
     * 订单统计
     * @param begin 开始日期
     * @param end 结束日期
     * @return
     */
    OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end);

    /**
     * 销量排行top10
     * @param begin 开始日期
     * @param end 结束日期
     * @return
     */
    SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end);
    /**
     * 导出运营数据报表
     * @param response
     */
    void exportBusinessData(HttpServletResponse response);

}
