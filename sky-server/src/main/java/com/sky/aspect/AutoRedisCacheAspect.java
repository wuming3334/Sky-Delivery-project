/*
package com.sky.aspect;

import com.sky.annotation.AutoRedisDelete;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Aspect
@Component
@Slf4j
public class AutoRedisCacheAspect {

    @Autowired
    private RedisTemplate redisTemplate;


    @Pointcut("@annotation(com.sky.annotation.AutoRedisDelete)")
    public void AutoRedisCachePointcut() {
    }

    */
/**
     * 在方法成功执行后，根据注解精准清理缓存
     *//*

    @AfterReturning(value = "@annotation(autoRedisDelete)", argNames = "joinPoint,autoRedisDelete")
    public void clearCache(JoinPoint joinPoint, AutoRedisDelete autoRedisDelete) {
        // 1. 获取方法的所有参数
        Object[] args = joinPoint.getArgs();

        // 2. 遍历参数，找到包含指定字段的那个对象 如DishDTO
        String categoryId = extractCategoryId(args, autoRedisDelete.categoryIdField());

        if (categoryId != null) {
            // 3. 拼接 Key 并删除
            String key = autoRedisDelete.keyPrefix() + categoryId;
            redisTemplate.delete(key);
            log.info("精准清理缓存: {}", key);
        }
    }

    */
/**
     * 提取 ID 的逻辑：通过反射从参数对象中获取指定的字段值
     *//*

    private String extractCategoryId(Object[] args, String fieldName) {
        for (Object arg : args) {
            if (arg == null) continue;

            try {
                // 尝试从当前参数对象中获取名为 categoryId 的字段
                Field field = arg.getClass().getDeclaredField(fieldName);
                field.setAccessible(true); // 允许访问私有字段
                Object value = field.get(arg);

                if (value != null) {
                    return value.toString();
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                // 如果这个参数里没有该字段，就跳过，看下一个参数
                continue;
            }
        }
        return null;
    }
}*/
