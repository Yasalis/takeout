package com.example.takeout.filter;

import com.alibaba.fastjson.JSON;
import com.example.takeout.common.BaseContext;
import com.example.takeout.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@WebFilter(filterName = "LoginCheckFilter", urlPatterns = "/*")
public class LoginCheckFilter implements Filter {

    //路径匹配器，支持通配符
    public static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        log.info("拦截到请求：{}", request.getRequestURI());

        String URI = request.getRequestURI();

        //放行路径
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/common/**",
                "/user/login",
                "/user/sendMsg",

                "/doc.html",
                "/webjars/**",
                "/swagger-resources",
                "/v2/api-docs"
        };

        boolean check = PathCheck(urls, URI);

        if (check) {
            log.info("放行 {}", URI);
            filterChain.doFilter(request, response);
            return;
        }
        //判断员工是否登录
        if (request.getSession().getAttribute("employee") != null) {
            log.info("用户已登录，用户id {}", request.getSession().getAttribute("employee"));

            long id = (long) request.getSession().getAttribute("employee");
            BaseContext.setCurrentId(id);

            filterChain.doFilter(request, response);
            return;
        }

        //判断用户是否登录
        if(request.getSession().getAttribute("user") != null){
            log.info("用户已登录，用户id为：{}",request.getSession().getAttribute("user"));
            Long userId = (Long)request.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);
            filterChain.doFilter(request,response);
            return;
        }
        log.info("用户未登录");
        response.getWriter().write(JSON.toJSONString(Result.error("NOTLOGIN")));
        return;
    }

    public boolean PathCheck(String[] urls, String requestURI) {
        for (String url : urls) {
            boolean match = ANT_PATH_MATCHER.match(url, requestURI);
            if (match) {
                return true;
            }
        }
        return false;
    }
}
