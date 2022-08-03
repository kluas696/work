package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import com.baomidou.mybatisplus.extension.api.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("admin/product/baseCategoryTrademark/")
@RestController
public class BseCategoryTrademarkController {
    @Autowired
    private BaseCategoryTrademarkService baseCategoryTrademarkService;

    //查询
    @GetMapping("findTrademarkList/{category3Id}")
    public Result findTrademarkList(@PathVariable Long category3Id){
        List<BaseTrademark> baseTrademarkList = baseCategoryTrademarkService.findTrademarkList(category3Id);
        return Result.ok(baseTrademarkList);
    }
    //逻辑删除
    @DeleteMapping("remove/{category3Id}/{trademarkId}")
    public Result removeTrademarkById(@PathVariable Long category3Id,
                                    @PathVariable Long trademarkId){
        baseCategoryTrademarkService.removeTrademarkById(category3Id,trademarkId);
        return Result.ok();
    }

    ///admin/product/baseCategoryTrademark/findCurrentTrademarkList/61
    @GetMapping("findCurrentTrademarkList/{category3Id}")
    public Result findCurrentTrademarkList(@PathVariable Long category3Id){
        List<BaseTrademark> trademarkList = baseCategoryTrademarkService.findCurrentTrademarkList(category3Id);
        return Result.ok(trademarkList);
    }
    //save
    @PostMapping("save")
    public Result save(@RequestBody CategoryTrademarkVo categoryTrademarkVo){
        baseCategoryTrademarkService.save(categoryTrademarkVo);
        return Result.ok();

    }
}
