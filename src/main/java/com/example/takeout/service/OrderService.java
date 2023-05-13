package com.example.takeout.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.takeout.entity.Orders;

public interface OrderService extends IService<Orders> {
    void submit(Orders orders);
}
