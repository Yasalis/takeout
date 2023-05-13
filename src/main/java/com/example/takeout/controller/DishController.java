package com.example.takeout.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.takeout.common.CustomException;
import com.example.takeout.common.Result;
import com.example.takeout.dto.DishDto;
import com.example.takeout.entity.Category;
import com.example.takeout.entity.Dish;
import com.example.takeout.entity.DishFlavor;
import com.example.takeout.service.CategoryService;
import com.example.takeout.service.DishFlavorService;
import com.example.takeout.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {
    @Resource
    private DishService dishService;

    @Resource
    private DishFlavorService dishFlavorService;

    @Resource
    private CategoryService categoryService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping
    public Result<String> save(@RequestBody DishDto dishDto){

        dishService.saveWithFlavor(dishDto);
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        stringRedisTemplate.delete(key);
        return Result.success("新增菜品成功！");
    }

    /***
     * 菜品信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public Result<Page> page(int page, int pageSize, String name){
        Page<Dish> pageInfo = new Page<>(page, pageSize);
        Page<DishDto> dishDtoPage = new Page<>();

        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(name != null, Dish::getName, name);
        //根据更新时间降序
        wrapper.orderByDesc(Dish::getUpdateTime);
        dishService.page(pageInfo, wrapper);
        //对象拷贝
        BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");

        List<Dish> records = pageInfo.getRecords();
        List<DishDto> list = records.stream().map((item) -> {
            DishDto dto = new DishDto();

            BeanUtils.copyProperties(item, dto);

            //获取分类id
            Long id = item.getCategoryId();

            //根据id查对象
            Category category = categoryService.getById(id);
            //应对数据库数据问题，有的数据查不到，直接执行会抛异常
            if(category != null){
                String categoryName = category.getName();
                dto.setCategoryName(categoryName);
            }
            return dto;
        }).collect(Collectors.toList());
        dishDtoPage.setRecords(list);
        return Result.success(dishDtoPage);
    }

    /***
     * 根据id查询菜品信息和口味
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<DishDto> get(@PathVariable Long id){

        DishDto dto = dishService.getByIdWithFlavor(id);
        return Result.success(dto);
    }

    @PutMapping
    public Result<String> updateDish(@RequestBody DishDto dishDto){
        dishService.updateWithFlavor(dishDto);
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        stringRedisTemplate.delete(key);
        return Result.success("修改成功！");
    }

    /**
     * 这里并不需要我们对删除操作也进行缓存清理，因为删除操作执行之前，必须先将菜品状态修改为停售，
     * 而停售状态也会帮我们清理缓存，同时也看不到菜品，随后将菜品删除，仍然看不到菜品，
     * 故删除操作不需要进行缓存清理
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    public Result<String> status(@PathVariable Integer status, @RequestParam List<Long> ids) {
        LambdaUpdateWrapper<Dish> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(ids != null, Dish::getId, ids);
        updateWrapper.set(Dish::getStatus, status);
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(Dish::getId, ids);
        List<Dish> dishes = dishService.list(lambdaQueryWrapper);
        for (Dish dish : dishes) {
            String key = "dish_" + dish.getCategoryId() + "_1";
            stringRedisTemplate.delete(key);
        }
        dishService.update(updateWrapper);
        return Result.success("批量操作成功");
    }


    /***
     * （这里是逻辑删除，不是真删，把isDeleted字段更新为1就不显示了，间接完成了逻辑删除）没实现
     * 直接删除
     * 用list接收要加 @RequestParam 注解
     * 批量删除
     * @param ids
     * @return
     */
    @DeleteMapping
    public Result<String> delete(@RequestParam List<Long> ids) {
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Dish::getId, ids);
        queryWrapper.eq(Dish::getStatus, 1);
        int count = dishService.count(queryWrapper);
        if (count > 0) {
            throw new CustomException("删除列表中存在启售状态商品，无法删除");
        }
        dishService.removeByIds(ids);
        return Result.success("删除成功");
    }

    /**
     * 根据分类查询菜品
     * @param dish
     * @return
     */
    @GetMapping("/list")
    public Result<List<DishDto>> list(Dish dish){
        List<DishDto> dishDtoList;
        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();
        //用 json 将存入 redis 中 json字符串 转换为 list 集合
        dishDtoList = JSON.parseObject(stringRedisTemplate.opsForValue().get(key), new TypeReference<List<DishDto>>(){});
        //如果有，则直接返回
        if (dishDtoList != null){
            return Result.success(dishDtoList);
        }

        //如果无，则查询
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
        wrapper.eq(Dish::getStatus, 1);
        wrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        List<Dish> list = dishService.list(wrapper);
        dishDtoList = list.stream().map((item) -> {
            //创建一个dishDto对象
            DishDto dto = new DishDto();
            //将item的属性全都copy到dishDto里
            BeanUtils.copyProperties(item, dto);
            //由于dish表中没有categoryName属性，只存了categoryId
            Long categoryId = item.getCategoryId();
            //所以我们要根据categoryId查询对应的category
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                //然后取出categoryName，赋值给dishDto
                dto.setCategoryName(category.getName());
            }
            //然后获取一下菜品id，根据菜品id去dishFlavor表中查询对应的口味，并赋值给dishDto
            Long itemId = item.getId();
            //条件构造器
            LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            //条件就是菜品id
            lambdaQueryWrapper.eq(itemId != null, DishFlavor::getDishId, itemId);
            //根据菜品id，查询到菜品口味
            List<DishFlavor> flavors = dishFlavorService.list(lambdaQueryWrapper);
            //赋给dishDto的对应属性
            dto.setFlavors(flavors);
            //并将dishDto作为结果返回
            return dto;
        }).collect(Collectors.toList());
        log.info("listDto >> {}", dishDtoList);

        //如果value需要存对象，可以将对象转换成json字符串存入。
        //将 dishDtoList 用 json 转换为 json字符串 存入redis
        String dishDtoListStr = JSON.toJSONString(dishDtoList);
        //将查询的结果让Redis缓存，设置存活时间为60分钟
        stringRedisTemplate.opsForValue().set(key, dishDtoListStr, 60, TimeUnit.MINUTES);

        return Result.success(dishDtoList);
    }
}
