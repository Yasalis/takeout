package com.example.takeout.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.takeout.common.CustomException;
import com.example.takeout.entity.Category;
import com.example.takeout.entity.Dish;
import com.example.takeout.entity.Setmeal;
import com.example.takeout.mapper.CategoryMapper;
import com.example.takeout.service.CategoryService;
import com.example.takeout.service.DishService;
import com.example.takeout.service.SetmealService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Resource
    private DishService dishService;
    @Resource
    private SetmealService setmealService;

    @Override
    public void remove(Long id) {
        LambdaQueryWrapper<Dish> dishWrapper = new LambdaQueryWrapper<>();
        //添加查询条件，根据分类id进行查询
        dishWrapper.eq(Dish::getCategoryId, id);
        int count1 = dishService.count(dishWrapper);

        //查询当前分类是否关联了菜品，如果已经关联，抛出一个业务异常
        if(count1 > 0){
            //已经关联了菜品，抛出异常
            throw new CustomException("当前分类下关联了菜品，不能删除");
        }

        //查询当前分类是否关联了套餐，如果已经关联，抛出一个业务异常
        LambdaQueryWrapper<Setmeal> setmealWrapper = new LambdaQueryWrapper<>();
        //添加查询条件，根据分类id进行查询
        setmealWrapper.eq(Setmeal::getCategoryId, id);
        int count2 = setmealService.count(setmealWrapper);
        if(count2 > 0){
            //已经关联了套餐，抛出异常
            throw new CustomException("当前分类下关联了套餐，不能删除");
        }

        //正常删除分类
        super.removeById(id);
    }
}
