package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/product/baseTrademark")
public class BaseTrademarkController {
    @Autowired
    private BaseTrademarkService baseTrademarkService;
    ;

    @GetMapping("{page}/{limit}")
    public Result getBaseTrademark(@PathVariable Long page,
                                   @PathVariable Long limit) {
        //封装查询条件
        Page<BaseTrademark> baseTrademarkPage = new Page<>(page, limit);
        IPage iPage = baseTrademarkService.getBaseTrademark(baseTrademarkPage);
        return Result.ok(iPage);
    }

    //save
    @PostMapping("save")
    public Result save(@RequestBody BaseTrademark baseTrademark) {
        baseTrademarkService.save(baseTrademark);
        return Result.ok();
    }

    //delete
    @DeleteMapping("remove/{id}")
    public Result removeById(@PathVariable String id) {
        baseTrademarkService.removeById(id);
        return Result.ok();
    }
    //update
    //更新之情需查询
    @GetMapping("get/{id}")
    public Result getBaseTrademarkById(@PathVariable String id){
        BaseTrademark baseTrademark = baseTrademarkService.getById(id);
        return Result.ok(baseTrademark);
    }

    @PutMapping("update")
    public Result update(@RequestBody BaseTrademark baseTrademark){
        baseTrademarkService.updateById(baseTrademark);
        return Result.ok();

    }


}
