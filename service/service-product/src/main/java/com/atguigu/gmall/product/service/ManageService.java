package com.atguigu.gmall.product.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.model.product.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ManageService {

    //查询一级分类
    List<BaseCategory1> getCategory1();

    //根据一级category1Id获取二级分类
    List<BaseCategory2> getCategory2(Long category1Id);

    //根据二级category1Id获取三级数据
    List<BaseCategory3> getCategory3(Long category2Id);

    //获取平台属性
    List<BaseAttrInfo> getArrInfoList(Long category1Id, Long category2Id, Long category3Id);

    //新增平台属性
    void saveArrInfo(BaseAttrInfo baseAttrInfo);

    //根据属性id,获取平台属性值集合
    List<BaseAttrValue> getAttrValueList(Long attrId);

    //先查询出属性，判断是否为空
    BaseAttrInfo getAttrInfo(Long attrId);

    /**
     * 根据分页参数获取分页列表
     * @param spuInfoPage
     * @param spuInfo
     * @return
     */
    IPage<SpuInfo> getSpuInfoPage(Page<SpuInfo> spuInfoPage, SpuInfo spuInfo);

    //查询销售属性
    List<BaseSaleAttr> getBaseSaleAttrList();
    //保存销售属性
    void saveSpuInfo(SpuInfo spuInfo);
    //回显spuImage列表
    List<SpuImage> getSpuImageList(Long spuId);
    //回显销售属性集合
    List<SpuSaleAttr> getSpuSaleAttrList(Long spuId);

    //保存sku
    void saveSkuInfo(SkuInfo skuInfo);

    /**
     * 获取skuInfo 分页
     * @param skuInfoPage
     * @return
     */
    IPage<SkuInfo> getSkuInfoPage(Page<SkuInfo> skuInfoPage);

    void onSale(Long skuId);

    void cancelSale(Long skuId);

    //给service-item模块提供接口
    SkuInfo getSkuInfo(Long skuId);

    BigDecimal getSkuPrice(Long skuId);

    //获取分类信息
    BaseCategoryView getCategoryView(Long category3Id);

    //获取销售属性、销售属性值并锁定
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId);

    //根据spuId来获取销售值Id组成的数据
    Map getSkuValueIdsMap(Long spuId);
    //获取海报信息
    List<SpuPoster> getSkuPosterBySpuId(long spuId);

    List<BaseAttrInfo> getAttrList(Long skuId);

    /**
     * 获取首页分类数据
     * @return
     */
    List<JSONObject> getCategoryList();

    BaseTrademark getTrademarkByTmId(Long tmId);
}
