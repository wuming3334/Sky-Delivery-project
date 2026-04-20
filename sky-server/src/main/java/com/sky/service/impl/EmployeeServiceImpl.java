package com.sky.service.impl;

import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.*;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;
    @Autowired
    private Properties pageHelperProperties;
    @Autowired
    private EmployeeService employeeService;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        //对传过来的密码进行md5加密
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        String pasw = password;
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    @Override
    public void save(EmployeeDTO employeeDTO) throws Exception {
        Employee employee = new Employee();
      /*  employee.setUsername(employeeDTO.getUsername());
        employee.setName(employeeDTO.getName());
        employee.setPhone(employeeDTO.getPhone());
        employee.setSex(employeeDTO.getSex());
        employee.setIdNumber(employeeDTO.getIdNumber());*/
        //属性拷贝
        BeanUtils.copyProperties(employeeDTO, employee);
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));
        employee.setStatus(StatusConstant.ENABLE);
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        //获取操作人的id
        Long empId = BaseContext.getCurrentId();
        if (empId != null) {
            employee.setCreateUser(empId);
            employee.setUpdateUser(empId);
        } else {
           throw new Exception("操作失败");
        }
        employeeMapper.insert(employee);
    }

    /**
     * 分页查询员工
     *
     * @param employeePageQueryDTO
     */
    @Override
    public PageResult page(EmployeePageQueryDTO employeePageQueryDTO) {
        //定义一个pagehelper对象
        PageHelper PH = new PageHelper();
        //设置参数
        PH.startPage(employeePageQueryDTO.getPage(),
                employeePageQueryDTO.getPageSize());
        //执行查询
      /*  List<Employee> list = employeeMapper.page(employeePageQueryDTO);*/
        List<Employee> list = employeeMapper.page(employeePageQueryDTO);
/*        Page<Employee> page = (Page<Employee>) list;*/
        //将结果蜂封装到list集合中
        PageResult pr = new PageResult();
        pr.setTotal(list.size());
        pr.setRecords(list);
        return pr;
    }

    /**
     * 修改员工账号状态
     * 0禁用 1启用
     * @param status
     * @param id
     */
    @Override
    public  void updateStatus(Integer status, Long id) throws Exception {
        //创建员工对象封装属性
       /* if (BaseContext.getCurrentId() != 1){
            throw new PermissionDeniedException();
        }*/
       /* Employee employee = new Employee();
        employee.setId(id);
        //设置状态
        employee.setStatus(status);
        //更新基本信息
        employee.setUpdateTime(LocalDateTime.now());
        employee.setUpdateUser(BaseContext.getCurrentId());*/
        Employee emp = Employee.builder()
                .id(id)
                .status(status)
                .updateTime(LocalDateTime.now())
                .updateUser(BaseContext.getCurrentId())
                .build();
        employeeMapper.update(emp);
    }

}
