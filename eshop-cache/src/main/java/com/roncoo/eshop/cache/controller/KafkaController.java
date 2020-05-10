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

@Api(tags = "Kafka消息测试接口", value = "发送Kafka消息异步加载Redis缓存")
@RestController
@Slf4j
public class KafkaController {

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    @ApiOperation("发送更新商品信息服务消息")
    @GetMapping("/sendProductInfoMsg")
    public String sendProductInfoMsg(@RequestParam Integer productId) {
        kafkaTemplate.send(
                KafkaConsumer.CACHE_TOPIC,
                "{\"serviceId\": \"productInfoService\",\"productId\": " + productId + "}"
        );
        return "success";
    }

    @ApiOperation("发送更新店铺信息服务消息")
    @PostMapping("/sendShopInfoMsg")
    public String sendShopInfoMsg(@RequestParam Integer shopId) {
        kafkaTemplate.send(
                KafkaConsumer.CACHE_TOPIC,
                "{\"serviceId\": \"shopInfoService\",\"shopId\": " + shopId + "}"
        );
        return "success";
    }


}
