package com.sky.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 按日期分组的统计数据DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateGroupDTO implements Serializable {
    
    // 日期
    private String date;
    
    // 数量或金额
    private BigDecimal value;
}
