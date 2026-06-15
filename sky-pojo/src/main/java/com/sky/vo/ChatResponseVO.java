package com.sky.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatResponseVO {
    private String content;       // 文本回复
    private String action;        // 执行的动作: add_cart, remove_cart, view_cart, place_order, view_order, none
    private Object actionResult;  // 动作结果数据
    private boolean done;         // 是否结束
}
