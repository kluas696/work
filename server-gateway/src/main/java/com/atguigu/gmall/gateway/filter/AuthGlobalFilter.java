package com.atguigu.gmall.gateway.filter;

import ch.qos.logback.core.db.dialect.DBUtil;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.config.ConfigurationUtils;
import org.springframework.data.redis.connection.ConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class AuthGlobalFilter implements GlobalFilter {
    @Autowired
    private RedisTemplate redisTemplate;

    // 匹配路径的工具类
    AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Value("${authUrls.url}")
    private String authUrls;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        /**
         * 1.用户未登录，限制trade.html,myOrder.html ,list.html ,
         * 2.禁止用户访问内部接口，带有inner,像 /api/list/inner/upperGoods/{skuId}
         *3.用户未登录，限制访问 带有 auth
         */

        //禁止用户访问内部接口，带有inner,

        //获取请求对象
        ServerHttpRequest request = exchange.getRequest();
        //获取path,getURI为获取全路径
        String path = request.getURI().getPath();

        //判断path是否有 inner
        if (antPathMatcher.match("/**/inner/**", path)) {
            //响应对象
            ServerHttpResponse response = exchange.getResponse();
            return out(response, ResultCodeEnum.PERMISSION);
        }

        //获取用户 userId
        String userId = getUserId(request);
        //获取用户 临时id
        String userTempId = getUserTempId(request);

        //
        if ("-1".equals(userId)) {
            //token不匹配
            //响应对象
            ServerHttpResponse response = exchange.getResponse();
            return out(response, ResultCodeEnum.PERMISSION);
        }

        //用户未登录，限制访问 带有 auth
        if (antPathMatcher.match("/api/**/auth/**", path)) {
            //判断用户是否登录
            if (StringUtils.isEmpty(userId)) {
                //响应对象
                ServerHttpResponse response = exchange.getResponse();
                return out(response, ResultCodeEnum.LOGIN_AUTH);
            }
        }

        //用户未登录，限制trade.html,myOrder.html ,list.html ,
        //分割，获取单独的  **.html
        String[] splits = authUrls.split(",");
        for (String url : splits) {
            //判断是否含有trade.html,myOrder.html ,list.html....之一
            //未登录的情况下，跳转
            if (path.indexOf(url) != -1 && StringUtils.isEmpty(userId)) {

                ServerHttpResponse response = exchange.getResponse();

                //地址
                response.getHeaders().set(HttpHeaders.LOCATION, "http://www.gmall.com/login.html?originUrl=" + request.getURI());

                //重定向到登录页面
                return response.setComplete();
            }

        }

        // 将userId 放入请求头
        if (!StringUtils.isEmpty(userId) || !StringUtils.isEmpty(userTempId)) {
            if (!StringUtils.isEmpty(userId)) {
                //登录状态
                request.mutate().header("userId", userId).build();}
            if (!StringUtils.isEmpty(userTempId)) {
                //未登录状态
                request.mutate().header("userTempId", userTempId).build();}
                // 将现在的request 变成 exchange对象
                return chain.filter(exchange.mutate().request(request).build());
            }
            return chain.filter(exchange);

        }


    //获取用户 临时id
    private String getUserTempId(ServerHttpRequest request) {
        String userTempId = "";
        HttpCookie httpCookie = request.getCookies().getFirst("userTempId");
        if (httpCookie != null) {
            userTempId = httpCookie.getValue();
        } else {
            List<String> stringList = request.getHeaders().get("userTempId");
            if (!CollectionUtils.isEmpty(stringList)) {
                userTempId = stringList.get(0);
            }
        }
        return userTempId;
    }


    //获取用户 userId
    private String getUserId(ServerHttpRequest  request) {
        //从coolies或者headers获取token
        String token = "";
        HttpCookie httpCookie = request.getCookies().getFirst("token");
        if (httpCookie != null) {
            token = httpCookie.getValue();
        } else {
            List<String> stringList = request.getHeaders().get("token");
            if (!CollectionUtils.isEmpty(stringList)) {
                token = stringList.get(0);
            }
        }
        //判断token是否为空
        if (!StringUtils.isEmpty(token)) {
            //从缓存中获取数据
            String userLoginKey = "user:login:" + token;
            String json = (String) redisTemplate.opsForValue().get(userLoginKey);
            //转为对象
            JSONObject userJson = JSONObject.parseObject(json);

            String ip = (String) userJson.get("ip");
            if (ip.equals(IpUtil.getGatwayIpAddress(request))) {
                String userId = (String) userJson.get("userId");
                return userId;
            }else {
                return "-1";
            }
        }
        return "";
    }






        /**
         * 限制用户访问内部接口
         * @param response
         * @param resultCodeEnum
         * @return
         */
        private Mono<Void> out (ServerHttpResponse response, ResultCodeEnum resultCodeEnum){
            // 返回用户没有权限登录
            Result<Object> result = Result.build(null, resultCodeEnum);
            byte[] bits = JSONObject.toJSONString(result).getBytes(StandardCharsets.UTF_8);
            DataBuffer wrap = response.bufferFactory().wrap(bits);
            response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
            // 输入到页面
            return response.writeWith(Mono.just(wrap));

        }

}
