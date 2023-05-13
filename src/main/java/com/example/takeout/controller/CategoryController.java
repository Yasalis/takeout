package com.example.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.takeout.common.Result;
import com.example.takeout.entity.Category;
import com.example.takeout.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/category")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    @PostMapping
    public Result<String> save(@RequestBody Category category){
        categoryService.save(category);
        return Result.success("新增菜品成功");
    }

    @GetMapping("/page")
    public Result<Page> page(int page, int pageSize){
        Page<Category> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Category::getSort);

        categoryService.page(pageInfo, wrapper);
        return Result.success(pageInfo);
    }

    @DeleteMapping
    public Result<String> delete(Long id){
        categoryService.remove(id);
        return Result.success("分类信息删除成功");
    }

    @PutMapping
    public Result<String> upData(@RequestBody Category category){
        categoryService.updateById(category);
        return Result.success("分类信息修改成功");
    }

    @GetMapping("/list")
    public Result<List<Category>> list(Category category){
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(category.getType() != null, Category::getType, category.getType());
        wrapper.orderByAsc(Category::getSort).orderByDesc(Category::getUpdateTime);

        List<Category> list = categoryService.list(wrapper);
        return Result.success(list);
    }

}
