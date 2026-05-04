package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    /**
     * 插入购物车数据
     * @param shoppingCart
     */
    void insert(ShoppingCart shoppingCart);

    /**
     * 根据用户id和菜品id或套餐id查询购物车
     * @param shoppingCart
     * @return
     */
    ShoppingCart getByUserIdAndDishIdOrSetmealId(ShoppingCart shoppingCart);
    /**
     * 根据用户id查询购物车
     */
    List<ShoppingCart> listByUserId(Long userId);
    /**
     * 根据id更新数量
     * @param shoppingCart
     */
    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void updateNumberById(ShoppingCart shoppingCart);

    /**
     * 根据用户id查询购物车列表
     * @param userId
     * @return
     */
    List<ShoppingCart> list(Long userId);

    /**
     * 根据用户id删除购物车数据
     * @param userId
     */
    void deleteByUserId(Long userId);

    /**
     * 根据id删除购物车数据
     * @param id
     */
    void deleteById(Long id);
}
