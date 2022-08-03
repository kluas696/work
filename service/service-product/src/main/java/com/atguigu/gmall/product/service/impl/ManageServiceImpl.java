package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ManageServiceImpl implements ManageService {
    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;
    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;
    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;
    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;
    @Autowired private SpuInfoMapper spuInfoMapper;
    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;
    @Autowired
    private SpuImageMapper spuImageMapper;
    @Autowired
    private SpuPosterMapper spuPosterMapper;
    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;
    @Autowired
    private SkuInfoMapper skuInfoMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    private SkuImageMapper skuImageMapper;
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;
    @Autowired
    private RabbitService rabbitService;

    //查询一级分类
    @Override
    public List<BaseCategory1> getCategory1() {
        return baseCategory1Mapper.selectList(null);
    }
    //根据一级category1Id获取二级分类
    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        QueryWrapper<BaseCategory2> baseCategory2QueryWrapper = new QueryWrapper<>();
        baseCategory2QueryWrapper.eq("category1_id",category1Id);
        List<BaseCategory2> baseCategory2List = baseCategory2Mapper.selectList(baseCategory2QueryWrapper);
        return baseCategory2List;
    }

    //根据二级category2Id获取三级数据
    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        QueryWrapper<BaseCategory3> baseCategory3QueryWrapper = new QueryWrapper<>();
        baseCategory3QueryWrapper.eq("category2_id",category2Id);
        List<BaseCategory3> baseCategory3List = baseCategory3Mapper.selectList(baseCategory3QueryWrapper);
        return baseCategory3List;
    }

    //获取平台属性
    @Override
    public List<BaseAttrInfo> getArrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.selectBaseArrInfoList(category1Id,category2Id,category3Id);
        return baseAttrInfoList;
    }

    //新增平台属性
    @Override
    @Transactional(rollbackFor = Exception.class)//若有异常，回滚事务
    public void saveArrInfo(BaseAttrInfo baseAttrInfo) {
        /**
         * 判断 baseAttrInfo.getId();
         * 如果为空，就是insert,
         * 如果不为空，就是更新,
         * 操作的为 base_attr_info 表
         */
        if (baseAttrInfo.getId() != null){
            //更新
            baseAttrInfoMapper.updateById(baseAttrInfo);

            //进行更新操作，操作的为 base_attr_value 表
            //先删除
            QueryWrapper<BaseAttrValue> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("attr_id",baseAttrInfo.getId());
            baseAttrValueMapper.delete(queryWrapper);
        }else{
        //插入属性
            baseAttrInfoMapper.insert(baseAttrInfo);
        }

        //获取平台属性值集合，操作的为 base_attr_value 表
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        //插入属性值
        if (!CollectionUtils.isEmpty(attrValueList)){
            for (BaseAttrValue baseAttrValue : attrValueList) {
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insert(baseAttrValue);

            }
        }

    }
    //根据属性id,获取平台属性值集合
    @Override
    public List<BaseAttrValue> getAttrValueList(Long attrId) {
        QueryWrapper<BaseAttrValue> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("attr_id",attrId);
        return baseAttrValueMapper.selectList(queryWrapper);

    }

    @Override
    public BaseAttrInfo getAttrInfo(Long attrId) {
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        //有平台属性
        if (baseAttrInfo != null){
            //将获取到的平台属性值集合放入baseAttrInfo
            baseAttrInfo.setAttrValueList(this.getAttrValueList(attrId));
        }
        return baseAttrInfo;
    }

    /**
     * 根据分页参数获取分页列表
     * @param spuInfoPage
     * @param spuInfo
     * @return
     */
    @Override
    public IPage<SpuInfo> getSpuInfoPage(Page<SpuInfo> spuInfoPage, SpuInfo spuInfo) {

        //查询条件
        QueryWrapper<SpuInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category3_id",spuInfo.getCategory3Id());
        queryWrapper.orderByDesc("id");
        return spuInfoMapper.selectPage(spuInfoPage,queryWrapper);
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        List<BaseSaleAttr> baseSaleAttrList = baseSaleAttrMapper.selectList(null);
        return baseSaleAttrList;

    }

    //保存销售属性
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSpuInfo(SpuInfo spuInfo) {
        //spuInfo
        //spuImage
        //spuSaleAttr
        //spuSaleAttrValue
        //spuPoster

        //自动映射到实体类
        spuInfoMapper.insert(spuInfo);

        //图片
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (!CollectionUtils.isEmpty(spuImageList)) {
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);
            }
        }
        //销售属性
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (!CollectionUtils.isEmpty(spuSaleAttrList)) {
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);

                //销售属性值
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (!CollectionUtils.isEmpty(spuSaleAttrValueList)) {
                    spuSaleAttrValueList.stream().forEach(spuSaleAttrValue -> {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        //销售属性名称
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    });
                }
            }

        }
        //海报
        List<SpuPoster> spuPosterList = spuInfo.getSpuPosterList();
        if (!CollectionUtils.isEmpty(spuPosterList)){
            for (SpuPoster spuPoster : spuPosterList) {
                spuPoster.setSpuId(spuInfo.getId());
                spuPosterMapper.insert(spuPoster);
            }

        }


    }
    //回显spuImage列表
    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
        QueryWrapper<SpuImage> wrapper = new QueryWrapper<>();
        wrapper.eq("spu_id",spuId);
        return spuImageMapper.selectList(wrapper);
    }

    //根据spuId获取销售属性集合(包含销售属性值集合)
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(Long spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);

    }
    //保存skuInfo
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSkuInfo(SkuInfo skuInfo) {

        //skuInfo
        skuInfoMapper.insert(skuInfo);

        //skuImage
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!CollectionUtils.isEmpty(skuImageList)) {
            skuImageList.stream().forEach(skuImage -> {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);
            });
        }

        //skuAttrValue
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)) {
            skuAttrValueList.forEach(skuAttrValue -> {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);
            });
        }
        //skuSaleAttrValue
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)) {
            skuSaleAttrValueList.stream().forEach(skuSaleAttrValue -> {
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            });
        }


    }

    /**
     * 获取skuInfo 分页
     * @param skuInfoPage
     * @return
     */
    @Override
    public IPage<SkuInfo> getSkuInfoPage(Page<SkuInfo> skuInfoPage) {
        QueryWrapper<SkuInfo> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("id");
        return skuInfoMapper.selectPage(skuInfoPage,wrapper);
    }
    //上架
    @Override
    @Transactional
    public void onSale(Long skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(1);
        skuInfoMapper.updateById(skuInfo);

        //商品上架
        //rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_UPPER, skuId);

    }
    //下架
    @Override
    public void cancelSale(Long skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(0);
        skuInfoMapper.updateById(skuInfo);

        //商品下架
        //rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_LOWER, skuId);
        }

    //给service-item模块提供接口

    @Override
    @GmallCache(prefix = "getSkuInfo:")
    public SkuInfo getSkuInfo(Long skuId) {


        return getSkuInfoDB(skuId);
    }

    /**
     * 从缓存中获取 redisson
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoRedisson(Long skuId) {
        //设置一个缓存key skuKey=sku:skuId:info
        String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
        try {
            //根据key获取value
            SkuInfo skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            if (skuInfo == null) {
                //解决缓存击穿问题
                if (skuKey != null) {
                    //设置分布式锁 lockKey=sku:skuId:lock
                    String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                    //redisson
                    RLock lock = redissonClient.getLock(lockKey);
                    //上锁
                    boolean b = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                    if (b) {
                        try {
                            //业务逻辑
                            //获取锁成功
                            skuInfo = getSkuInfoDB(skuId);
                            //若为空
                            if (skuInfo == null) {
                                //为防止缓存穿透，设置null,并设置过期时间
                                SkuInfo skuInfo1 = new SkuInfo();
                                this.redisTemplate.opsForValue().set(skuKey, skuInfo1, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                                return skuInfo1;
                            }
                            //若不为空
                            this.redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                            return skuInfo;
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            //解锁
                            lock.unlock();
                        }
                    } else {
                        //上锁失败
                        Thread.sleep(100);
                        return getSkuInfo(skuId);
                    }
                } else {
                    //缓存中有数据
                    return skuInfo;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return getSkuInfoDB(skuId);
    }

    /**
     * 从缓存中获取 Redis
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoRedis(Long skuId) {
        //设置一个缓存key skuKey=sku:skuId:info
        String skuKey = RedisConst.SKUKEY_PREFIX+ skuId + RedisConst.SKUKEY_SUFFIX;
        //根据key获取value
        SkuInfo skuInfo= (SkuInfo) redisTemplate.opsForValue().get(skuKey);

        try {
            //判断skuInfo是否为空,若为空，查询数据库
            if (skuInfo == null) {
                //解决缓存击穿问题
                if (skuKey != null) {
                    //设置分布式锁 lockKey=sku:skuId:lock
                    String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                    //声明token口令串
                    String token = UUID.randomUUID().toString();
                    //从数据库中查询
                    Boolean aBoolean = this.redisTemplate.opsForValue().setIfAbsent(lockKey, token, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);
                    if (aBoolean) {
                        //获取锁成功
                        skuInfo = getSkuInfoDB(skuId);
                        //若为空
                        if (skuInfo == null) {
                            //为防止缓存穿透，设置null,并设置过期时间
                            SkuInfo skuInfo1 = new SkuInfo();
                            this.redisTemplate.opsForValue().set(skuKey, skuInfo1, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                            return skuInfo1;
                        }
                        //若不为空
                        this.redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                        //删除分布式锁
                        //  定义一个lua 脚本
                        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

                        //  准备执行lua 脚本
                        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                        //  将lua脚本放入DefaultRedisScript 对象中
                        redisScript.setScriptText(script);
                        //  设置DefaultRedisScript 这个对象的泛型
                        redisScript.setResultType(Long.class);
                        //  执行删除
                        redisTemplate.execute(redisScript, Arrays.asList("lockKey"), token);
                        return skuInfo;
                    } else {
                        //获取锁失败
                        Thread.sleep(100);
                        return getSkuInfo(skuId);
                    }
                } else {
                    //若skuInfo不为null
                    return skuInfo;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return getSkuInfoDB(skuId);
    }


    /**
     * 从数据库中获取
     */
    private SkuInfo getSkuInfoDB(Long skuId) {
        //取出skuInfo基本信息
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        //根据skuId获取skuImage图片列表
        QueryWrapper<SkuImage> wrapper = new QueryWrapper<>();
        wrapper.eq("sku_id", skuId);
        List<SkuImage> skuImageList = skuImageMapper.selectList(wrapper);
        if (skuInfo!=null){
            skuInfo.setSkuImageList(skuImageList);
        }
        return skuInfo;
    }

    @Override
    public BigDecimal getSkuPrice(Long skuId) {

        //集群用分布式锁
        RLock lock = redissonClient.getLock(skuId + "lock");
        lock.lock();

        try {
            QueryWrapper<SkuInfo> wrapper = new QueryWrapper<>();
            wrapper.eq("id",skuId);
            //查询单个字段
            wrapper.select("price");
            SkuInfo skuInfo = skuInfoMapper.selectOne(wrapper);
            if (skuInfo != null){
                return skuInfo.getPrice();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return new BigDecimal(0);
    }
    //获取分类信息
    @Override
    @GmallCache(prefix = "getCategoryView:")
    public BaseCategoryView getCategoryView(Long category3Id) {

        return baseCategoryViewMapper.selectById(category3Id);
    }
    //获取销售属性、销售属性值并锁定
    @Override
    @GmallCache(prefix = "getSpuSaleAttrListCheckBySku:")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {

        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId,spuId);

    }

    @Override
    @GmallCache(prefix = "getSkuValueIdsMap:")
    public Map getSkuValueIdsMap(Long spuId) {

        HashMap<Object, Object> hashMap = new HashMap<>();

        List<Map> mapList = skuSaleAttrValueMapper.selectSkuValueIdsMapBySpuId(spuId);
        if (!CollectionUtils.isEmpty(mapList)){
            for (Map map : mapList) {
                hashMap.put(map.get("value_ids"),map.get("sku_id"));
            }
        }

        return hashMap;
    }
    //获取海报信息
    @Override
    @GmallCache(prefix = "getSkuPosterBySpuId:")
    public List<SpuPoster> getSkuPosterBySpuId(long spuId) {
        return spuPosterMapper.selectList(new QueryWrapper<SpuPoster>().eq("spu_id",spuId));
    }

    @Override
    @GmallCache(prefix = "getAttrList:")
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        return baseAttrInfoMapper.selectAttrList(skuId);
    }

    /**
     * 获取首页分类数据
     * @return
     */
    @Override
    @GmallCache(prefix ="getCategoryList:" )
    public List<JSONObject> getCategoryList() {
        List<JSONObject> list = new ArrayList<>();
        //从视图中查出所有分类数据
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
        //根据一级分类id进行分组,查询出一级分类集合数据
        Map<Long, List<BaseCategoryView>> category1Map =
                baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        //
        int index = 1;
        for (Map.Entry<Long, List<BaseCategoryView>> entry1 : category1Map.entrySet()) {
            //获取一级分类id
            Long category1Id = entry1.getKey();
            //一级分类id对应的数据集合
            List<BaseCategoryView> categoryViewList1 = entry1.getValue();
            //创建一个一级分类对象，存储一级分类数据
            JSONObject category1 = new JSONObject();
            //一级分类id
            category1.put("index",index);
            category1.put("categoryId",category1Id);
            //一级分类名称
            category1.put("categoryName",categoryViewList1.get(0).getCategory1Name());

            index++;

            //查询出二级分类数据
            Map<Long, List<BaseCategoryView>> category2Map = categoryViewList1.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            //声明一个集合存储二级分类对象
            List<JSONObject> categoryChild2List = new ArrayList<>();
            for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2Map.entrySet()) {
                //获取二级分类id
                Long category2Id = entry2.getKey();
                //二级分类数据
                List<BaseCategoryView> categoryViewList2 = entry2.getValue();
                //创建一个二级分类对象，存储二级分类数据
                JSONObject category2 = new JSONObject();
                category2.put("categoryId",category2Id);
                category2.put("categoryName",categoryViewList2.get(0).getCategory2Name());

                categoryChild2List.add(category2);


                //三级分类数据
                //声明一个集合存储三级分类对象
                List<JSONObject> categoryChild3List = new ArrayList<>();
                categoryViewList2.forEach(baseCategoryView -> {
                    //创建一个二级分类对象，存储二级分类数据
                    JSONObject category3 = new JSONObject();
                    category3.put("categoryId",baseCategoryView.getCategory3Id());
                    category3.put("categoryName",baseCategoryView.getCategory3Name());
                    categoryChild3List.add(category3);
                });
                //绑定二级分类数据
                category2.put("categoryChild",categoryChild3List);
            }
            //放入对应的二级分类数据
            category1.put("categoryChild",categoryChild2List);
            list.add(category1);
        }

        return list;
    }

    @Override
    public BaseTrademark getTrademarkByTmId(Long tmId) {
        return baseTrademarkMapper.selectById(tmId);
    }


}
