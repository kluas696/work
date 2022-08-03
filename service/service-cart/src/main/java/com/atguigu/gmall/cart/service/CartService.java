package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

public interface CartService {


    /**
     * 根据用户Id 查询购物车列表
     *
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

    void addToCart(Long skuId, String userId, Integer skuNum);

    List<CartInfo> getCartList(String userId, String userTempId);

    /**
     * 更新选中状态
     *
     * @param userId
     * @param isChecked
     * @param skuId
     */
    void checkCart(String userId, Integer isChecked, Long skuId);

    void deleteCart(Long skuId, String userId);

    void delCart(String userId);
}
