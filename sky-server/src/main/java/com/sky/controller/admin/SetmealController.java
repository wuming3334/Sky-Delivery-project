package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.Cacheable;

import java.util.List;
import java.util.Set;

/**
 * 套餐管理
 */
@RestController
@RequestMapping("/admin/setmeal")
@Api(tags = "套餐相关接口")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;
    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 添加套餐
     */
    @PostMapping
    @CacheEvict(cacheNames = "setmealCache", key = "#setmealDTO.categoryId" )
    public Result save(@RequestBody SetmealDTO setmealDTO) {
        setmealService.insertSetmeal(setmealDTO);
 /*       redisTemplate.delete("setmeal_" + setmealDTO.getCategoryId());*/
        return Result.success();
    }

    /**
     * 修改套餐
     *
     * @param setmealDTO
     * @return
     */

    @PutMapping
    @ApiOperation("修改套餐")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result update(@RequestBody SetmealDTO setmealDTO) {
        log.info("修改套餐：{}", setmealDTO);
        setmealService.updateWithDishes(setmealDTO);
        redisTemplate.delete("setmeal_" + setmealDTO.getCategoryId());
        return Result.success();
    }

    /**
     * 套餐批量删除
     *
     * @param ids
     * @return
     */

    @DeleteMapping
    @ApiOperation("套餐批量删除")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result delete(@RequestParam List<Long> ids) {
        log.info("套餐批量删除：{}", ids);
        // 先查询分类ID
        java.util.Set<String> keysToDelete = new java.util.HashSet<>();
        for (Long id : ids) {
            com.sky.entity.Setmeal setmeal = setmealService.getById(id);
            if (setmeal != null) {
                keysToDelete.add("setmeal_" + setmeal.getCategoryId());
            }
        }
        // 再删除数据库
        setmealService.deleteBatch(ids);
      /*  // 最后删除缓存
        if (!keysToDelete.isEmpty()) {
            redisTemplate.delete(keysToDelete);
        }*/
        return Result.success();
    }

    /**
     * 套餐分页查询
     *
     * @param setmealPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("套餐分页查询")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("套餐分页查询：{}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据id查询套餐
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询套餐")
    public Result<SetmealVO> getById(@PathVariable Long id) {
        log.info("根据id查询套餐：{}", id);
        SetmealVO setmealVO = setmealService.getByIdWithDishes(id);
        return Result.success(setmealVO);
    }

    /**
     * 套餐起售停售
     *
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("套餐起售停售")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result startOrStop(@PathVariable Integer status, Long id) {
        log.info("套餐起售停售：{}, {}", status, id);
        setmealService.startOrStop(status, id);
        com.sky.entity.Setmeal setmeal = setmealService.getById(id);
        if (setmeal != null) {
            redisTemplate.delete("setmeal_" + setmeal.getCategoryId());
        }
        return Result.success();
    }
}
