package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import org.springframework.stereotype.Service;

import java.util.List;


public interface ShoppingCartService {
    /**
     * 添加购物车
     */
    public void add(ShoppingCartDTO shoppingCartDTO);

    /**
     * 查询购物车列表
     * @return
     */
    List<ShoppingCart> list();
}

