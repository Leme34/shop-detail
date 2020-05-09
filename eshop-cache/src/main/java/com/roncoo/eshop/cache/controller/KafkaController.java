package com.roncoo.eshop.cache.controller;

import com.roncoo.eshop.cache.kafka.KafkaConsumer;
import com.roncoo.eshop.cache.model.ProductInfo;
import com.roncoo.eshop.cache.model.ShopInfo;
import com.roncoo.eshop.cache.service.CacheService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Api(tags = "Kafka消息测试Controller", value = "发送Kafka消息异步加载Redis缓存")
@RestController
@Slf4j
public class KafkaController {

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    @ApiOperation("发送商品信息服务消息")
    @PostMapping("/sendProductInfoMsg")
    public String sendProductInfoMsg() {
        kafkaTemplate.send(
                KafkaConsumer.CACHE_TOPIC,
                "{\"serviceId\": \"productInfoService\",\"productId\": 1}"
        );
        return "success";
    }

    @ApiOperation("发送店铺信息服务消息")
    @PostMapping("/sendShopInfoMsg")
    public String sendShopInfoMsg() {
        kafkaTemplate.send(
                KafkaConsumer.CACHE_TOPIC,
                "{\"serviceId\": \"shopInfoService\",\"shopId\": 1}"
        );
        return "success";
    }


}
