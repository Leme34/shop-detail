package com.roncoo.eshop.cache.controller;

import com.alibaba.fastjson.JSONObject;
import com.roncoo.eshop.cache.model.ProductInfo;
import com.roncoo.eshop.cache.model.ShopInfo;
import com.roncoo.eshop.cache.service.CacheService;
import com.roncoo.eshop.cache.task.RebuildCacheQueue;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Api(tags = "商品信息服务接口")
@RestController
@Slf4j
public class CacheController {

    @Resource
    private CacheService cacheService;

    @ApiOperation("测试EhCache写请求")
    @PostMapping("/testPutCache")
    public String testPutCache(ProductInfo productInfo) {
        cacheService.saveLocalCache(productInfo);
        return "success";
    }

    @ApiOperation("测试EhCache读请求")
    @GetMapping("/testGetCache")
    public ProductInfo testGetCache(Long id) {
        return cacheService.getLocalCache(id);
    }


    /* ============================================================================  */


    @ApiOperation("商品信息查询")
    @GetMapping("/getProductInfo")
    public ProductInfo getProductInfo(Long productId) {
        ProductInfo productInfo;
        // 1 先查询Redis缓存
        productInfo = cacheService.getProductInfoFromRedisCache(productId);
        log.debug("从redis中获取缓存，商品信息={}", productInfo);

        // 2 未命中Redis缓存，再查询本地ehcache缓存
        if (productInfo == null) {
            productInfo = cacheService.getProductInfoFromLocalCache(productId);
            log.debug("从ehcache中获取缓存，商品信息={}", productInfo);
        }

        // 3 若缓存都未命中，则需要从数据库重新读数据，并重建缓存
        if (productInfo == null) {
            // 模拟数据库查询返回的结果
            String productInfoJSON = "{\"id\": 2, \"name\": \"iphone7手机\", \"price\": 5599, \"pictureList\":\"a.jpg,b.jpg\", \"specification\": \"iphone7的规格\", \"service\": \"iphone7的售后服务\", \"color\": \"红色,白色,黑色\", \"size\": \"5.5\", \"shopId\": 1, \"modifiedTime\": \"2017-01-01 12:00:00\"}";
            productInfo = JSONObject.parseObject(productInfoJSON, ProductInfo.class);
            // 放入缓存重建队列
            RebuildCacheQueue.getInstance().putProductInfo(productInfo);
            log.debug("缓存都未命中，重建缓存，从数据库重新读取的商品信息={}", productInfo);
        }

        return productInfo;
    }

    @ApiOperation("店铺信息查询")
    @GetMapping("/getShopInfo")
    public ShopInfo getShopInfo(Long shopId) {
        ShopInfo shopInfo;
        // 1 先查询Redis缓存
        shopInfo = cacheService.getShopInfoFromRedisCache(shopId);
        log.debug("从redis中获取缓存，店铺信息={}", shopInfo);

        // 2 未命中Redis缓存，再查询本地ehcache缓存
        if (shopInfo == null) {
            shopInfo = cacheService.getShopInfoFromLocalCache(shopId);
            log.debug("从ehcache中获取缓存，店铺信息={}", shopInfo);
        }

        // 3 若缓存都未命中，则需要从数据库重新读数据，并重建缓存
        if (shopInfo == null) {
            // TODO
        }

        return shopInfo;
    }

}
