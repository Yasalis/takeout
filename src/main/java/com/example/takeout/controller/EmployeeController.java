package com.example.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.takeout.common.Result;
import com.example.takeout.entity.Employee;
import com.example.takeout.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Resource
    private EmployeeService employeeService;

    @PostMapping("/login")
    public Result<Employee> login(HttpServletRequest request, @RequestBody Employee employee){
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername, employee.getUsername());
        Employee emp = employeeService.getOne(queryWrapper);
        if (emp == null || !emp.getPassword().equals(password)){
            return Result.error("登录失败");
        }
        if(emp.getStatus() == 0){
            return Result.error("账号已禁用");
        }
        request.getSession().setAttribute("employee", emp.getId());
        return Result.success(emp);
    }

    @PostMapping("logout")
    public Result<String> logOut(HttpServletRequest request){
        request.getSession().removeAttribute("employee");
        return Result.success("退出成功");
    }

    @PostMapping("")
    public Result<String> save(HttpServletRequest request, @RequestBody Employee employee){

        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        Long id = (Long) request.getSession().getAttribute("employee");
        employee.setCreateUser(id);
        employee.setUpdateUser(id);
        employeeService.save(employee);
        return Result.success("新增员工成功");
    }

    @GetMapping("/page")
    public Result<Page> page(int page, int pageSize, String name){
        Page pageInfo = new Page(page, pageSize);
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotEmpty(name), Employee::getName, name);
        wrapper.orderByDesc(Employee::getUpdateTime);
        employeeService.page(pageInfo, wrapper);
        return Result.success(pageInfo);
    }

    @PutMapping
    public Result<Employee> upData(@RequestBody Employee employee){
        employeeService.updateById(employee);
        return Result.success(employee);
    }

    @GetMapping("/{id}")
    public Result<Employee> getById(@PathVariable Long id){
        Employee employee = employeeService.getById(id);
        if(employee != null){
            return Result.success(employee);
        }
        return Result.error("没有查询到对应员工的信息");
    }
    

}
