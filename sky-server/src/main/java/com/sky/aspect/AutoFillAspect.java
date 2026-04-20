package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;

import static com.sky.constant.AutoFillConstant.*;


/**
 * 自定义切面类,实现自动填充
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    /**
     * 切入点
     */

    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill) ")
    public void autoFillPointcut() {
    }

    //前置通知 为公共字段赋值
    @Before("autoFillPointcut()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始进行公共字段自动填充...");
        //获取数据库操作类型
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

               /* public static final String SET_CREATE_TIME = "setCreateTime";
                public static final String SET_UPDATE_TIME = "setUpdateTime";
                public static final String SET_CREATE_USER = "setCreateUser";
                public static final String SET_UPDATE_USER = "setUpdateUser";*/
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
        }
    }
}
