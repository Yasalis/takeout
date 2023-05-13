package com.example.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.takeout.common.MailUtils;
import com.example.takeout.common.Result;
import com.example.takeout.entity.User;
import com.example.takeout.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("/sendMsg")
    public Result<String> sendMsg(@RequestBody User user, HttpSession session) throws MessagingException {
        String phone = user.getPhone();
        if (!phone.isEmpty()) {
            //随机生成一个验证码
            String code = MailUtils.achieveCode();
            log.info(code);
            //这里的phone其实就是邮箱，code是我们生成的验证码
            MailUtils.sendTestMail(phone, code);
            //验证码缓存到Redis，设置存活时间3分钟
            stringRedisTemplate.opsForValue().set(phone, code,3, TimeUnit.MINUTES);
            return Result.success("验证码发送成功");
        }
        return Result.error("验证码发送失败");
    }

    @PostMapping("/login")
    public Result<User> login(@RequestBody Map map, HttpSession session){
        //获取邮箱
        String phone = map.get("phone").toString();
        //获取验证码
        String code = map.get("code").toString();
        ///把Redis中缓存的code拿出来
        String codeInRedis = stringRedisTemplate.opsForValue().get(phone);
        //看看接收到用户输入的验证码是否和redis中的验证码相同
        log.info("你输入的code{}，redis中的code{}，计算结果为{}", code, codeInRedis, (code != null && code.equals(codeInRedis)));
        if (code != null && code.equals(codeInRedis)) {
            //如果输入正确，判断一下当前用户是否存在
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            //判断依据是从数据库中查询是否有其邮箱
            queryWrapper.eq(User::getPhone, phone);
            User user = userService.getOne(queryWrapper);
            //如果不存在，则创建一个，存入数据库
            if (user == null) {
                user = new User();
                user.setPhone(phone);
                user.setName("用户" + codeInRedis);
                userService.save(user);
            }
            //存个session，表示登录状态
            session.setAttribute("user",user.getId());
            //如果登录成功，则删除Redis中的验证码
            stringRedisTemplate.delete(phone);
            //并将其作为结果返回
            return Result.success(user);
        }
        return Result.error("登录失败");
    }

    @PostMapping("/loginout")
    public Result<String> logout(HttpServletRequest request) {
        request.getSession().removeAttribute("user");
        return Result.success("退出成功");
    }

}
