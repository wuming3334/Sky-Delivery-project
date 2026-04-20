package com.sky.service;

import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.result.PageResult;

public interface EmployeeService {

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 添加员工
     *
     * @param
     */
    void save(EmployeeDTO employeeDTO) throws Exception;

    /**
     * 分页查询员工
     *
     * @param employeePageQueryDTO
     */
    PageResult page(EmployeePageQueryDTO employeePageQueryDTO);

    void updateStatus(Integer status, Long id) throws Exception;
    Employee getById(Long id);
    public void update(EmployeeDTO employeeDTO);
}
