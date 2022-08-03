package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BaseCategoryTrademarkService extends IService<BaseCategoryTrademark> {

     List<BaseTrademark> findTrademarkList(Long category3Id);


    void removeTrademarkById(Long category3Id, Long trademarkId);

    /**
     * 查询出未关联的品牌id
     * @param category3Id
     * @return
     */
    List<BaseTrademark> findCurrentTrademarkList(Long category3Id);

    void save(CategoryTrademarkVo categoryTrademarkVo);
}
