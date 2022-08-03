package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.mapper.BaseCategoryTrademarkMapper;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BaseCategoryTrademarkServiceImpl extends ServiceImpl<BaseCategoryTrademarkMapper,BaseCategoryTrademark> implements BaseCategoryTrademarkService {
    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;
    @Autowired
    private BaseCategoryTrademarkMapper baseCategoryTrademarkMapper;

    @Override
    public List<BaseTrademark> findTrademarkList(Long category3Id) {
        //先根据category3Id，查询base_category_trademark表，再获取trademark_id，然后查询base_trademark表
        QueryWrapper<BaseCategoryTrademark> wrapper = new QueryWrapper<>();
        wrapper.eq("category3_id", category3Id);
        List<BaseCategoryTrademark> baseCategoryTrademarkList = baseCategoryTrademarkMapper.selectList(wrapper);
        //判断集合是否为空
        if (!CollectionUtils.isEmpty(baseCategoryTrademarkList)) {
            //取出trademark_id集合
            List<Long> trademarkIdList = baseCategoryTrademarkList.stream().map(baseCategoryTrademark -> {
                return baseCategoryTrademark.getTrademarkId();
            }).collect(Collectors.toList());
            //查询出数据
            return baseTrademarkMapper.selectBatchIds(trademarkIdList);
        }
        //若集合为空，返回null
        return null;
    }

    //逻辑删除品牌
    //UPDATE base_category_trademark SET is_deleted=1 WHERE is_deleted=0 AND (category3_id = ? AND trademark_id = ?)
    @Override
    public void removeTrademarkById(Long category3Id, Long trademarkId) {
        //封装删除条件
        QueryWrapper<BaseCategoryTrademark> wrapper = new QueryWrapper<>();
        wrapper.eq("category3_id", category3Id);
        wrapper.eq("trademark_id", trademarkId);
        baseCategoryTrademarkMapper.delete(wrapper);
    }

    /**
     * 查询出未关联的品牌id
     *
     * @param category3Id
     * @return
     */
    @Override
    public List<BaseTrademark> findCurrentTrademarkList(Long category3Id) {
        //先查出关联的品牌id
        QueryWrapper<BaseCategoryTrademark> wrapper = new QueryWrapper<>();
        wrapper.eq("category3_id", category3Id);
        List<BaseCategoryTrademark> baseCategoryTrademarkList = baseCategoryTrademarkMapper.selectList(wrapper);

        //判断集合是否为空
        if (!CollectionUtils.isEmpty(baseCategoryTrademarkList)) {
            //查出关联的品牌id
            List<Long> trademarkIdList = baseCategoryTrademarkList.stream().map(baseCategoryTrademark -> {
                return baseCategoryTrademark.getTrademarkId();
            }).collect(Collectors.toList());
            //查找出所有的品牌id,过滤掉已经关联的
            List<BaseTrademark> baseTrademarkList = baseTrademarkMapper.selectList(null).stream().filter(baseTrademark -> {
                return !trademarkIdList.contains(baseTrademark.getId());
            }).collect(Collectors.toList());
            return baseTrademarkList;
        }
        return null;
    }

    @Override
    public void save(CategoryTrademarkVo categoryTrademarkVo) {
        
        //获取品牌id集合
        List<Long> trademarkIdList = categoryTrademarkVo.getTrademarkIdList();
        if (!CollectionUtils.isEmpty(trademarkIdList)){

           /* List<BaseCategoryTrademark> baseCategoryTrademarkList = new ArrayList<>();
            //遍历
            for (Long trademarkId : trademarkIdList) {
                //创建一个分类id与品牌关联的对象
                BaseCategoryTrademark baseCategoryTrademark = new BaseCategoryTrademark();
                baseCategoryTrademark.setCategory3Id(categoryTrademarkVo.getCategory3Id());
                baseCategoryTrademark.setTrademarkId(trademarkId);
                //将遍历出来的对象加入到集合中
                //baseCategoryTrademarkList.add(baseCategoryTrademark);
                //baseCategoryTrademarkMapper.insert(baseCategoryTrademark);
            }*/

            List<BaseCategoryTrademark> baseCategoryTrademarkList = trademarkIdList.stream().map(trademarkId -> {
                BaseCategoryTrademark baseCategoryTrademark = new BaseCategoryTrademark();
                baseCategoryTrademark.setCategory3Id(categoryTrademarkVo.getCategory3Id());
                baseCategoryTrademark.setTrademarkId(trademarkId);
                return baseCategoryTrademark;
            }).collect(Collectors.toList());
            this.saveBatch(baseCategoryTrademarkList);

        }

    }

}


