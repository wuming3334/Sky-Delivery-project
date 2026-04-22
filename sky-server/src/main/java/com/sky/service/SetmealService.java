package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {

    /**
     * 修改套餐
     * @param setmealDTO
     */
    void updateWithDishes(SetmealDTO setmealDTO);

    /**
     * 套餐批量删除
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 根据id查询套餐和关联的菜品数据
     * @param id
     * @return
     */
    SetmealVO getByIdWithDishes(Long id);

    /**
     * 添加套餐
     */
    void insertSetmeal( SetmealDTO setmealDTO);
}
