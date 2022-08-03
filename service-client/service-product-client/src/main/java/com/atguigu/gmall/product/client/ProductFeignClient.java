package com.atguigu.gmall.product.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.impl.ProductDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient(value = "service-product",fallback = ProductDegradeFeignClient.class)
public interface ProductFeignClient {

    //skuInfo
    @GetMapping("admin/product/inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId);
    //获取价格
    @GetMapping("admin/product/inner/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId);
    //获取分类信息
    @GetMapping("admin/product/inner/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id);
    //获取销售属性、销售属性值并锁定
    @GetMapping("admin/product/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId,
                                                          @PathVariable Long spuId);
    //根据spuId来获取销售值Id组成的数据
    @GetMapping("admin/product/inner/getSkuValueIdsMap/{spuId}")
    public Map getSkuValueIdsMap(@PathVariable Long spuId);
    //获取海报信息
    @GetMapping("admin/product/inner/getSkuPosterBySpuId/{spuId}")
    public List<SpuPoster> getSkuPosterBySpuId(@PathVariable long spuId);
    //规格与包装
    @GetMapping("admin/product/inner/getAttrList/{skuId}")
    public List<BaseAttrInfo> getAttrList(@PathVariable Long skuId);

    //首页显示分类
    @GetMapping("/admin/product/getBaseCategoryList")
    public Result getBaseCategoryList();
    //品牌
    @GetMapping("/admin/product/inner/getTrademark/{tmId}")
    public BaseTrademark getTrademark(@PathVariable("tmId")Long tmId);
}
