package com.atguigu.gmall.order.controller;

import com.alibaba.nacos.common.utils.StringUtils;
import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/order")
public class OrderApiController {

    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private CartFeignClient cartFeignClient;
    @Autowired
    private OrderService orderService;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;


    /**
     * 确认订单
     * @param request
     * @return
     */
    @GetMapping("auth/trade")
    public Result<Map<String, Object>> trade(HttpServletRequest request) {

        Map<String,Object> map = new HashMap<>();

        //用户信息
        String userId = AuthContextHolder.getUserId(request);
        //用户地址
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);

        //totalNum totalAmount detailArrayList

        //用户的商品
        List<OrderDetail> orderDetailList = new ArrayList<>();
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);

        List<OrderDetail> orderDetails = cartCheckedList.stream().map(cartInfo -> {

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());
            // 添加到集合
            orderDetailList.add(orderDetail);

            return orderDetail;
        }).collect(Collectors.toList());

        //数量
        int totalNum = orderDetails.size();

        //总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetails);
        orderInfo.sumTotalAmount();

        //放入map
        map.put("userAddressList",userAddressList);
        map.put("detailArrayList",orderDetailList);
        map.put("totalNum",totalNum);
        map.put("totalAmount",orderInfo.getTotalAmount());

        //订单流水号
        map.put("tradeNo",orderService.getTradeNo(userId));

        return Result.ok(map);
    }
    /**
     * 提交订单
     * @param orderInfo
     * @param request
     * @return
     */
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request){

        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));

        //校验流水号
        //获取前台流水号
        String tradeNo = request.getParameter("tradeNo");
        Boolean checkTradeNo = orderService.checkTradeNo(userId, tradeNo);
        if (!checkTradeNo){
            return Result.fail().message("去回退刷新");
        }
        //删除流水号
        orderService.deleteTradeNo(userId);

        List<String> errorList = new ArrayList<>();
        List<CompletableFuture> futureList = new ArrayList<>();
        // 验证库存：
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            CompletableFuture<Void> checkStockCompletableFuture = CompletableFuture.runAsync(() -> {
                // 验证库存：
                boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
                if (!result) {
                    errorList.add(orderDetail.getSkuName() + "库存不足！");
                }
            }, threadPoolExecutor);
            futureList.add(checkStockCompletableFuture);

            CompletableFuture<Void> checkPriceCompletableFuture = CompletableFuture.runAsync(() -> {
                // 验证价格：
                BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
                if (orderDetail.getOrderPrice().compareTo(skuPrice) != 0) {
                    // 重新查询价格！
                    List<CartInfo> cartCheckedList = this.cartFeignClient.getCartCheckedList(userId);
                    //  写入缓存：
                    cartCheckedList.forEach(cartInfo -> {
                        this.redisTemplate.opsForHash().put(RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX, cartInfo.getSkuId().toString(), cartInfo);
                    });
                    errorList.add(orderDetail.getSkuName() + "价格有变动！");
                }
            }, threadPoolExecutor);
            futureList.add(checkPriceCompletableFuture);
        }

        //合并线程
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[futureList.size()])).join();
        if(errorList.size() > 0) {
            return Result.fail().message(StringUtils.join(errorList, ","));
        }

        //提交订单
        Long orderId = orderService.submitOrder(orderInfo);
        return Result.ok(orderId);

    }

    /**
     * 我的订单
     * @return
     */
    @GetMapping("auth/{page}/{limit}")
    public Result<IPage<OrderInfo>> index(@PathVariable Long page,
                                          @PathVariable Long limit,
                                          HttpServletRequest request){

        //获取用户id
        String userId = AuthContextHolder.getUserId(request);

        Page<Object> pageParam = new Page<>(page, limit);
        IPage iPage = orderService.getPage(pageParam,userId);


        return Result.ok(iPage);
    }

}
