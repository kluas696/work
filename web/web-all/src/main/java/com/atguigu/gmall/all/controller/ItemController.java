package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.ItemFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
public class ItemController {

    @Autowired
    private ItemFeignClient itemFeignClient;

    @RequestMapping("{skuId}.html")
    public String itemIndex(@PathVariable Long skuId, Model model){
        //远程调用service-item 汇总的数据
        Result<Map> result = itemFeignClient.getItem(skuId);
        //获取数据
        model.addAllAttributes(result.getData());

        //返回商品详情的视图名称
        return "item/item";
    }
}
