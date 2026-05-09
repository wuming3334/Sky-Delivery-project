package com.sky.context;

public class BaseContext {

    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();
    // 存储当前登录员工的权限
    public static ThreadLocal<String> currentRole = new ThreadLocal<>();
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void removeCurrentId() {

        threadLocal.remove();
    }

    /**
     * 获取员工权限级别
     */
    public static void setCurrentRole(String role) {
        currentRole.set(role);
    }
    public static String getCurrentRole (){
        return currentRole.get();
    }
    public static void removeCurrentRole() {
        currentRole.remove();
    }

}
