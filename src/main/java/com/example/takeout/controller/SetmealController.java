package com.example.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.takeout.common.Result;
import com.example.takeout.dto.DishDto;
import com.example.takeout.dto.SetmealDto;
import com.example.takeout.entity.Category;
import com.example.takeout.entity.Dish;
import com.example.takeout.entity.Setmeal;
import com.example.takeout.entity.SetmealDish;
import com.example.takeout.service.CategoryService;
import com.example.takeout.service.DishService;
import com.example.takeout.service.SetmealDishService;
import com.example.takeout.service.SetmealService;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/setmeal")
public class SetmealController {
    @Resource
    private SetmealService setmealService;

    @Resource
    private CategoryService categoryService;

    @Resource
    private SetmealDishService setmealDishService;

    @Resource
    private DishService dishService;

    @PostMapping
    public Result<String> save(@RequestBody SetmealDto setmealDto){
        setmealService.saveWithDish(setmealDto);
        return Result.success("新增套餐成功");
    }

    @GetMapping("/page")
    public Result<Page> page(int page, int pageSize, String name){
        Page<Setmeal> pageInfo = new Page<>(page, pageSize);
        Page<SetmealDto> dtoPage = new Page<>();
        LambdaQueryWrapper<Setmeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(name != null, Setmeal::getName, name);
        //根据更新时间降序排序
        wrapper.orderByDesc(Setmeal::getUpdateTime);
        setmealService.page(pageInfo, wrapper);

        BeanUtils.copyProperties(pageInfo, dtoPage, "records");
        List<Setmeal> setmeals = pageInfo.getRecords();
        List<SetmealDto> list = setmeals.stream().map((item) -> {
            SetmealDto dto = new SetmealDto();
            BeanUtils.copyProperties(item, dto);
            Long id = item.getCategoryId();
            Category category = categoryService.getById(id);
            if(category != null){
                String categoryName = category.getName();
                dto.setCategoryName(categoryName);
            }
            return dto;
        }).collect(Collectors.toList());
        dtoPage.setRecords(list);
        return Result.success(dtoPage);
    }

    @DeleteMapping
    public Result<String> delete(List<Long> ids){
        setmealService.removeWithDish(ids);
        return Result.success("删除成功");
    }

    /**
     * 这俩都是更新状态操作，一个启售一个停售
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    public Result<String> status(@PathVariable String status, @RequestParam List<Long> ids) {
        LambdaUpdateWrapper<Setmeal> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Setmeal::getId, ids);
        updateWrapper.set(Setmeal::getStatus, status);
        setmealService.update(updateWrapper);
        return Result.success("批量操作成功");
    }

    @GetMapping("/list")
    public Result<List<Setmeal>> list(Setmeal setmeal) {
        //条件构造器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        //添加条件
        queryWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus() != null, Setmeal::getStatus, setmeal.getStatus());
        //排序
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        List<Setmeal> setmealList = setmealService.list(queryWrapper);
        return Result.success(setmealList);
    }

    @GetMapping("/dish/{id}")
    public Result<List<DishDto>> showSetmealDish(@PathVariable Long id) {
        //条件构造器
        LambdaQueryWrapper<SetmealDish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //手里的数据只有setmealId
        dishLambdaQueryWrapper.eq(SetmealDish::getSetmealId, id);
        //查询数据
        List<SetmealDish> records = setmealDishService.list(dishLambdaQueryWrapper);
        List<DishDto> dtoList = records.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            //copy数据
            BeanUtils.copyProperties(item,dishDto);
            //查询对应菜品id
            Long dishId = item.getDishId();
            //根据菜品id获取具体菜品数据，这里要自动装配 dishService
            Dish dish = dishService.getById(dishId);
            //其实主要数据是要那个图片，不过我们这里多copy一点也没事
            BeanUtils.copyProperties(dish,dishDto);
            return dishDto;
        }).collect(Collectors.toList());

        return Result.success(dtoList);
    }

    /**
     * 套餐修改（获取要修改的套餐）
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<SetmealDto> getById(@PathVariable Long id) {
        Setmeal setmeal = setmealService.getById(id);
        SetmealDto setmealDto = new SetmealDto();
        //拷贝数据
        BeanUtils.copyProperties(setmeal, setmealDto);
        //条件构造器
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        //根据setmealId查询具体的setmealDish
        queryWrapper.eq(SetmealDish::getSetmealId, id);
        List<SetmealDish> setmealDishes = setmealDishService.list(queryWrapper);
        //然后再设置属性
        setmealDto.setSetmealDishes(setmealDishes);
        //作为结果返回
        return Result.success(setmealDto);
    }

    /**
     * 套餐修改
     * @param setmealDto
     * @return
     */
    @PutMapping
    public Result<Setmeal> updateWithDish(@RequestBody SetmealDto setmealDto) {
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        Long setmealId = setmealDto.getId();
        //先根据id把setmealDish表中对应套餐的数据删了
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId,setmealId);
        setmealDishService.remove(queryWrapper);
        //然后在重新添加
        setmealDishes = setmealDishes.stream().map((item) ->{
            //这属性没有，需要我们手动设置一下
            item.setSetmealId(setmealId);
            return item;
        }).collect(Collectors.toList());
        //更新套餐数据
        setmealService.updateById(setmealDto);
        //更新套餐对应菜品数据
        setmealDishService.saveBatch(setmealDishes);
        return Result.success(setmealDto);
    }
}
