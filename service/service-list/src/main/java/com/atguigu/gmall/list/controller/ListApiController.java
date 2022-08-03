package com.atguigu.gmall.list.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;


/**
 * @author atguigu-mqx
 */
@RestController
@RequestMapping("/api/list")
public class ListApiController {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private SearchService searchService;

    @GetMapping("inner/createIndex")
    public Result createIndex(){
        //  创建mapping！ 过时的方法！能用！{7.x} 意思是还有更好的！{启动的时候，会自动创建！}
        //  底层源码使用的RestHighLevelClient 高级客户端！
        restTemplate.createIndex(Goods.class);
        restTemplate.putMapping(Goods.class);
        return Result.ok();
    }

    //  商品的上架：
    @GetMapping("inner/upperGoods/{skuId}")
    public Result upperGoods(@PathVariable Long skuId){
        searchService.upperGoods(skuId);
        return Result.ok();
    }

    //  商品的下架：
    @GetMapping("inner/lowerGoods/{skuId}")
    public Result lowerGoods(@PathVariable Long skuId){
        searchService.lowerGoods(skuId);
        return Result.ok();
    }


    //  /api/list/inner/incrHotScore/{skuId}
    @GetMapping("/inner/incrHotScore/{skuId}")
    public Result incrHotScore(@PathVariable Long skuId){
        //  调用
        searchService.incrHotScore(skuId);
        return Result.ok();
    }

    //  查询控制器！
    //  @RequestBody  将json 数据转换为java 对象！
    @PostMapping()
    public Result list(@RequestBody SearchParam searchParam) throws  IOException {
        //  调用查询方法！
        SearchResponseVo searchResponseVo = searchService.search(searchParam);
        //  返回数据
        return Result.ok(searchResponseVo);
    }

}
