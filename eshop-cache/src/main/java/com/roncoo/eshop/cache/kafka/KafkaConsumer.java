package com.roncoo.eshop.cache.kafka;

import com.alibaba.fastjson.JSONObject;
import com.roncoo.eshop.cache.model.ProductInfo;
import com.roncoo.eshop.cache.model.ShopInfo;
import com.roncoo.eshop.cache.service.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Kafka消息监听器
 * <p>
 * Created by lsd
 * 2020-05-08 22:13
 */
@Slf4j
@Component
public class KafkaConsumer {

    @Resource
    private CacheService cacheService;

    @KafkaListener(topics = "cache-message")
    public void handlerMessage(String message) {
        // 首先将message转换成json对象
        JSONObject msgJSONObject = JSONObject.parseObject(message);

        // 从这里提取出消息对应的服务的标识
        String serviceId = msgJSONObject.getString("serviceId");

        // 不同服务的消息进行不同处理【缓存维度化】
        switch (serviceId) {
            case "productInfoService":   //商品信息服务
                this.processProductInfoChangeMessage(msgJSONObject);
                break;
            case "shopInfoService":      //店铺信息服务
                this.processShopInfoChangeMessage(msgJSONObject);
                break;
            default:
                break;
        }
    }

    /**
     * 处理【商品信息】变更的消息
     *
     * @param msgJSONObject 消息体 Json 对象
     */
    private void processProductInfoChangeMessage(JSONObject msgJSONObject) {
        // 提取出商品id
        Long productId = msgJSONObject.getLong("productId");

        // 调用商品信息服务的接口，例如：getProductInfo?productId=1
        // 商品信息服务一般来说会去查询数据库，获取此productId的商品信息，然后返回回来
        String productInfoJSON = "{\"id\": 1, \"name\": \"iphone7手机\", \"price\": 5599, \"pictureList\":\"a.jpg,b.jpg\", \"specification\": \"iphone7的规格\", \"service\": \"iphone7的售后服务\", \"color\": \"红色,白色,黑色\", \"size\": \"5.5\", \"shopId\": 1}";
        ProductInfo productInfo = JSONObject.parseObject(productInfoJSON, ProductInfo.class);
        cacheService.saveProductInfo2LocalCache(productInfo);
        log.debug("已保存到本地缓存的商品信息：{}", cacheService.getProductInfoFromLocalCache(productId));
        cacheService.saveProductInfo2RedisCache(productInfo);
    }

    /**
     * 处理【店铺信息】变更的消息
     *
     * @param msgJSONObject 消息体 Json 对象
     */
    private void processShopInfoChangeMessage(JSONObject msgJSONObject) {
        // 提取出店铺id
        Long shopId = msgJSONObject.getLong("shopId");

        // 调用店铺信息服务的接口，例如：getShopInfo?shopId=1
        // 店铺信息服务一般来说会去查询数据库，获取此shopId的店铺信息，然后返回回来
        String shopInfoJSON = "{\"id\": 1, \"name\": \"老王的手机店\", \"level\": 5, \"goodCommentRate\":0.99}";
        ShopInfo shopInfo = JSONObject.parseObject(shopInfoJSON, ShopInfo.class);
        cacheService.saveShopInfo2LocalCache(shopInfo);
        log.debug("已保存到本地缓存的店铺信息：{}", cacheService.getShopInfoFromLocalCache(shopId));
        cacheService.saveShopInfo2RedisCache(shopInfo);
    }


}
