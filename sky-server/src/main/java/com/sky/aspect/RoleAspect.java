package com.sky.aspect;

import com.sky.annotation.RequireRole;
import com.sky.context.BaseContext;
import com.sky.exception.PermissionDeniedException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RoleAspect {

    @Around("@annotation(requireRole)")
    public Object checkRole(ProceedingJoinPoint pjp, RequireRole requireRole) throws Throwable {
        // 1. 从 ThreadLocal 获取当前登录员工的角色
        String role = BaseContext.getCurrentRole();

        // 2. 校验
        if (!"ADMIN".equals(role)) {
            throw new PermissionDeniedException();
        }

        // 3. 放行
        return pjp.proceed();
    }
}