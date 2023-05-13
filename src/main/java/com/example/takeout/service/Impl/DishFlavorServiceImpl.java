package com.example.takeout.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.takeout.entity.DishFlavor;
import com.example.takeout.mapper.DishFlavorMapper;
import com.example.takeout.service.DishFlavorService;
import com.example.takeout.service.DishService;
import org.springframework.stereotype.Service;

@Service
public class DishFlavorServiceImpl extends ServiceImpl<DishFlavorMapper, DishFlavor> implements DishFlavorService {
}
