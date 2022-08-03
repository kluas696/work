package com.atguigu.gmall.order.service.impl;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderMapper;
import com.atguigu.gmall.order.service.OrderService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    RabbitService rabbitService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitOrder(OrderInfo orderInfo) {

        //order_info表中相关信息
        orderInfo.sumTotalAmount();
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + "" + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfo.setCreateTime(new Date());
        // 定义为1天
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        orderInfo.setExpireTime(calendar.getTime());
        orderInfo.setTradeBody("今年要消费！");
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());



        orderMapper.insert(orderInfo);

        //订单相关信息
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {

            orderDetail.setOrderId(orderInfo.getId());

            orderDetailMapper.insert(orderDetail);
        }
        //删除购物车数据
        //this.cartFeignClient.deleteCart(orderInfo.getUserId().toString());

        //返回订单id
        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {
        String tradeNo = UUID.randomUUID().toString();
        String tradeKey = "tradeNo:" + userId;
        redisTemplate.opsForValue().set(tradeKey,tradeNo);

        return tradeNo;
    }

    @Override
    public Boolean checkTradeNo(String userId, String tradeNo) {

        String tradeKey = "tradeNo:" + userId;
        String redisTradeNo = (String) redisTemplate.opsForValue().get(tradeKey);
        return tradeNo.equals(redisTradeNo);
    }

    @Override
    public void deleteTradeNo(String userId) {

        String tradeKey = "tradeNo:" + userId;
        redisTemplate.delete(tradeKey);
    }

    @Override
    public Boolean checkStock(Long skuId, Integer skuNum) {
        String result = HttpClientUtil.doGet("http://localhost:9001/hasStock?skuId=" + skuId + "&num=" + skuNum);
        return "1".equals(result);
    }

    /**
     * 获取订单分页信息
     * @param pageParam
     * @param userId
     * @return
     */
    @Override
    public IPage getPage(Page<Object> pageParam, String userId) {
        IPage<OrderInfo> page = orderMapper.selectPageByUserId(pageParam,userId);

        //设置状态名
        page.getRecords().stream().forEach(orderInfo -> {
            orderInfo.setOrderStatusName(OrderStatus.getStatusNameByStatus(orderInfo.getOrderStatus()));
        });

        return page;
    }




}
