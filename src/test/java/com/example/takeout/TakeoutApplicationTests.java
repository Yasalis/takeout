package com.example.takeout;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.takeout.dto.DishDto;
import com.example.takeout.entity.Dish;
import com.example.takeout.service.DishService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class TakeoutApplicationTests {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private DishService dishService;

    @Test
    void eq() {
        stringRedisTemplate.opsForValue().set("code","666",1, TimeUnit.MINUTES);
        String code = stringRedisTemplate.opsForValue().get("code");
        System.out.println("666".equals(code));
    }

    @Test
    void listRedis(){
        String key = "dish_1413384954989060097_1";
        DishDto dto1 = new DishDto();

        dto1.setFlavors(new ArrayList<>());
        dto1.setCopies(null);
        dto1.setCategoryName("主食");


        List<DishDto> value = new ArrayList<>();
        value.add(dto1);

        String strSet = JSON.toJSONString(value);
        System.out.println(strSet);

        stringRedisTemplate.opsForValue().set(key, strSet);

        String strGet = stringRedisTemplate.opsForValue().get(key);
        List<DishDto> res = JSON.parseObject(strGet, new TypeReference<List<DishDto>>(){});
        System.out.println(res.toString());
    }

}
