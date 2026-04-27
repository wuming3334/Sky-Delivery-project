/*
package com.sky.aspect;

import com.sky.annotation.AutoRedisDelete;
import com.sky.dto.DishDTO;
import com.sky.enumeration.OperationType;
import com.sky.enumeration.OperationType2;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import static com.sky.constant.AutoFillConstant.*;
import static com.sky.constant.AutoFillConstant.SET_UPDATE_USER;


*/
/**
 * 自定义切面类,实现自动填充
 *//*

*/
/*@Aspect
@Component*//*

@Slf4j
public class AutoRedisDeleteAspect {
    @Autowired

    private RedisTemplate redisTemplate;
    @Autowired
    private DishService dishService;

    */
/**
     * 切入点
     *//*


    @Pointcut(" @annotation(com.sky.annotation.AutoRedisDelete) ")
    public void autoRedisDeletePointcut() {
    }

    //前置通知 为公共字段赋值
    @Before("autoRedisDeletePointcut()")
    public void autoRedisDelete(JoinPoint joinPoint) {
        log.info("开始进行自动删除缓存...");
        */
/*//*
/获取数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();//方法签名对象
        //获取方法上的注释
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        //获取目标参数----实体对象  约定实体类型放在形参第一位
        Object[] args = joinPoint.getArgs();
        if (autoFill == null) {
            return;
        }
        //获取实体对象
        Object entity = args[0];
        //准备赋值的数据
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        //填充属性
        try {
            if (autoFill.value() == OperationType.INSERT) {

               *//*

        */
/* public static final String SET_CREATE_TIME = "setCreateTime";
                public static final String SET_UPDATE_TIME = "setUpdateTime";
                public static final String SET_CREATE_USER = "setCreateUser";
                public static final String SET_UPDATE_USER = "setUpdateUser";*//*

        */
/*
             //设置创建时间
              entity.getClass().getDeclaredMethod(SET_CREATE_TIME, LocalDateTime.class).invoke(entity, now);
                //设置更新时间
                entity.getClass().getDeclaredMethod(SET_UPDATE_TIME, LocalDateTime.class).invoke(entity, now);
                //设置创建人
                entity.getClass().getDeclaredMethod(SET_CREATE_USER, Long.class).invoke(entity, currentId);
                //设置更新人
                entity.getClass().getDeclaredMethod(SET_UPDATE_USER, Long.class).invoke(entity, currentId);


            } else if (autoFill.value() == OperationType.UPDATE) {
                //设置更新时间
                entity.getClass().getDeclaredMethod(SET_CREATE_TIME, LocalDateTime.class).invoke(entity, now);
                //设置更新人
                entity.getClass().getDeclaredMethod(SET_UPDATE_USER, Long.class).invoke(entity, currentId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*//*

        //查询redis缓存中是否有数据
        //判断获取的是什么 数据
        //获取数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();//方法签名对象
        //获取方法上的注释
        AutoRedisDelete autoRedisDelete = signature.getMethod().getAnnotation(AutoRedisDelete.class);
        //获取目标参数----实体对象  约定实体类型放在形参第一位
        Object[] args = joinPoint.getArgs();
        if (autoRedisDelete == null) {
            return;
        }
        //获取实体对象
        Object entity = args[0];
        //删除缓存
        try {
            if (autoRedisDelete.value() == OperationType2.INSERT || autoRedisDelete.value() == OperationType2.UPDATE) {
                DishDTO dishDTO = (DishDTO) entity;
                String key = "dish_" + dishDTO.getCategoryId();
                List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);
                redisTemplate.delete(key);
            } else if (autoRedisDelete.value() == OperationType2.DELETE) {
                List<Long> ids = (List<Long>) entity;
                List<DishVO> dishVOList = dishService.getByIdsWithFlavor(ids);
                java.util.Set<String> keysToDelete = new java.util.HashSet<>();
                for (DishVO dishVO : dishVOList) {
                    keysToDelete.add("dish_" + dishVO.getCategoryId());
                }
                if (!keysToDelete.isEmpty()) {
                    redisTemplate.delete(keysToDelete);
                }
            } else if (autoRedisDelete.value() == OperationType2.START_OR_STOP) {
                entity = args[1];
                Long id = (Long) entity;
                //根据id查询菜品
                DishVO dishVO = dishService.getByIdWithFlavor(id);
                String key = "dish_" + dishVO.getCategoryId();
                //若有,删除
                redisTemplate.delete(key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
*/
