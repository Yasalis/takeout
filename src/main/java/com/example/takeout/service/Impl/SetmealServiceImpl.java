package com.example.takeout.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.takeout.common.CustomException;
import com.example.takeout.dto.SetmealDto;
import com.example.takeout.entity.Setmeal;
import com.example.takeout.entity.SetmealDish;
import com.example.takeout.mapper.SetmealMapper;
import com.example.takeout.service.SetmealDishService;
import com.example.takeout.service.SetmealService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {
    @Resource
    private SetmealDishService setmealDishService;

    //新增套餐，同时保存套餐和菜品的关联关系
    @Override
    @Transactional
    public void saveWithDish(SetmealDto setmealDto) {
        //保存套餐基本信息，操作setmeal
        this.save(setmealDto);

        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes = setmealDishes.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());
        //保存套餐和菜品的关联关系，操作setmeal_dish
        setmealDishService.saveBatch(setmealDishes);

    }

    @Override
    @Transactional
    public void removeWithDish(List<Long> ids) {
        //查询状态，看能不能删除
        LambdaQueryWrapper<Setmeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Setmeal::getId, ids);
        wrapper.eq(Setmeal::getStatus, 1);
        //不能删除就抛异常
        int count = this.count(wrapper);
        if(count > 0){
            throw new CustomException("套餐正在售卖中，不能删除");
        }
        //如果能删除，先删除套餐表里的数据
        this.removeByIds(ids);
        //删除关系表中的数据
        LambdaQueryWrapper<SetmealDish> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.in(SetmealDish::getSetmealId, ids);
        setmealDishService.remove(wrapper1);
    }
}
