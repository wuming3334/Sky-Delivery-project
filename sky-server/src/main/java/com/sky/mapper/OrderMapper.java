package com.sky.mapper;

import com.sky.dto.DateGroupDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.List;

@Mapper
public interface OrderMapper {
    /**
     * 插入订单数据
     *
     * @param orders
     */

    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     *
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     *
     * @param orders
     */
    void update(Orders orders);

    /**
     * 根据状态和下单时间查询订单超时
     */
    @Select("select * from orders where status = #{status} and order_time < #{time}")
    List<Orders> getByStatusAndOrderTimeLT(Integer status, LocalDateTime time);

    /**
     * 批量更新
     */
    void batchCancelTimeoutOrders(List<Orders> orders);

    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    List<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据状态统计订单数量
     */
    Integer countByStatus(Integer status);

    /**
     * 根据时间范围和状态统计营业额（旧方法，保留兼容）
     */
    Double sumByTimeAndStatus(LocalDateTime begin, LocalDateTime end, Integer status);

    /**
     * 批量查询营业额统计（按日期分组）
     */
    List<DateGroupDTO> sumAmountGroupByDate(LocalDateTime begin, LocalDateTime end, Integer status);

    /**
     * 根据时间范围统计订单数量（旧方法，保留兼容）
     */
    Integer countByTime(LocalDateTime begin, LocalDateTime end);

    /**
     * 批量查询订单统计（按日期分组）
     */
    List<DateGroupDTO> countOrderGroupByDate(LocalDateTime begin, LocalDateTime end);

    /**
     * 批量查询有效订单统计（按日期分组）
     */
    List<DateGroupDTO> countValidOrderGroupByDate(LocalDateTime begin, LocalDateTime end, List<Integer> statusList);
}
