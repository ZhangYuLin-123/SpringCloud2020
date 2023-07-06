package com.zyl.springcloud.dao;

import com.zyl.springcloud.entities.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper // 是ibatis下面的注解 //@Repositoty有时候会有问题
public interface PaymentDao {

    int create(Payment payment);

    Payment getPaymentById(@Param("id") Long id);
}
