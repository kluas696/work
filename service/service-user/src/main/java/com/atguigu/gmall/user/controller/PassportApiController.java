package com.atguigu.gmall.user.controller;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.IPUtil;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.IpUtil;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user/passport/")
public class PassportApiController {
    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 用户登录
     * @param userInfo
     * @param request
     * @return
     */
    @PostMapping("login")
    public Result userInfo(@RequestBody UserInfo userInfo, HttpServletRequest request){
        UserInfo info = userService.login(userInfo);
        if (info!=null){
            //存入缓存
            String token = UUID.randomUUID().toString();
            //创建hash对象
            HashMap<String,Object> map = new HashMap<>();
            map.put("nickName",info.getNickName());
            map.put("token",token);

            JSONObject userJson = new JSONObject();
            userJson.put("userId",info.getId().toString());
            userJson.put("ip", IpUtil.getIpAddress(request));
            redisTemplate.opsForValue().set(RedisConst.USER_LOGIN_KEY_PREFIX + token,userJson.toString(),RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);


            return Result.ok(map);

        }else {
            return Result.fail().message("登录失败");
        }
    }

    /**
     * 用户退出
     */
    @GetMapping("logout")
    public Result logout(HttpServletRequest request){
        redisTemplate.delete(RedisConst.USER_KEY_PREFIX + request.getHeader("token"));
        return Result.ok();
    }
}
