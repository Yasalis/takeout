package com.example.takeout.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.takeout.dto.DishDto;
import com.example.takeout.entity.Dish;

public interface DishService extends IService<Dish> {
    //新增菜品，同时添加菜品对应的口味，要对dish、dish_flavor两张表操作
    public void saveWithFlavor(DishDto dishDto);

    //根据id查询菜品信息
    public DishDto getByIdWithFlavor(Long id);

    //更新菜品信息，同时更新口味
    public void updateWithFlavor(DishDto dishDto);
}
