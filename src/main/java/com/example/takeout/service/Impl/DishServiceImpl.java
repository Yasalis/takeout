package com.example.takeout.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.takeout.dto.DishDto;
import com.example.takeout.entity.Dish;
import com.example.takeout.entity.DishFlavor;
import com.example.takeout.mapper.DishMapper;
import com.example.takeout.service.DishFlavorService;
import com.example.takeout.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {
    @Resource
    private DishFlavorService dishFlavorService;

    @Override
    @Transactional//事务控制
    public void saveWithFlavor(DishDto dishDto) {
        //保存菜品的基本信息到菜品表dish
        this.save(dishDto);

        //菜品ID
        Long dishId = dishDto.getId();

        List<DishFlavor> list = dishDto.getFlavors();
        //从前端获取的list中没有dishId，把dishId封装进list里
        list = list.stream().map((item) -> {
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());

        //保存菜品的口味到菜品口味表dish_flavor
        dishFlavorService.saveBatch(list);

    }

    @Override
    public DishDto getByIdWithFlavor(Long id) {
        //查询菜品基本信息
        Dish dish = this.getById(id);

        DishDto dto = new DishDto();
        BeanUtils.copyProperties(dish, dto);

        //查询口味信息
        LambdaQueryWrapper<DishFlavor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishFlavor::getDishId, dish.getId());
        List<DishFlavor> list = dishFlavorService.list(wrapper);
        dto.setFlavors(list);
        return dto;
    }

    @Override
    @Transactional
    public void updateWithFlavor(DishDto dishDto) {
        //更新基本菜品信息
        this.updateById(dishDto);
        //清理当前菜品对应的口味信息————dish_flavor的delete方法
        LambdaQueryWrapper<DishFlavor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishFlavor::getDishId, dishDto.getId());
        dishFlavorService.remove(wrapper);

        //添加提交过来的口味数据————dish_flavor的insert方法
        List<DishFlavor> flavors = dishDto.getFlavors();
        flavors = flavors.stream().map((item) ->{
            item.setDishId(dishDto.getId());
            return item;
        }).collect(Collectors.toList());
        dishFlavorService.saveBatch(flavors);

    }
}
