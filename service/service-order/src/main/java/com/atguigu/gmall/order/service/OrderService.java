package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface OrderService {

    //插入订单
    Long submitOrder(OrderInfo orderInfo);

    String getTradeNo(String userId);

    //判断
    Boolean checkTradeNo(String userId,String tradeNo);

    void deleteTradeNo(String userId);

    /**
     * 验证库存
     * @param skuId
     * @param skuNum
     * @return
     */
    Boolean checkStock(Long skuId, Integer skuNum);

    IPage getPage(Page<Object> pageParam, String userId);


}
