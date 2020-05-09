package com.roncoo.eshop.cache.controller;

import com.roncoo.eshop.cache.model.ProductInfo;
import com.roncoo.eshop.cache.model.ShopInfo;
import com.roncoo.eshop.cache.service.CacheService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Api(tags = "EhCache缓存测试Controller")
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


    @GetMapping("/getProductInfo")
    public ProductInfo getProductInfo(Long productId) {
        ProductInfo productInfo;

        productInfo = cacheService.getProductInfoFromRedisCache(productId);
        log.debug("从redis中获取缓存，商品信息={}", productInfo);

        if (productInfo == null) {
            productInfo = cacheService.getProductInfoFromLocalCache(productId);
            log.debug("从ehcache中获取缓存，商品信息={}", productInfo);
        }

        if (productInfo == null) {
            // 就需要从数据源重新拉去数据，重建缓存，但是这里先不讲
        }

        return productInfo;
    }

    @GetMapping("/getShopInfo")
    public ShopInfo getShopInfo(Long shopId) {
        ShopInfo shopInfo;

        shopInfo = cacheService.getShopInfoFromRedisCache(shopId);
        log.debug("从redis中获取缓存，店铺信息={}", shopInfo);

        if (shopInfo == null) {
            shopInfo = cacheService.getShopInfoFromLocalCache(shopId);
            log.debug("从ehcache中获取缓存，店铺信息={}", shopInfo);
        }

        if (shopInfo == null) {
            // 就需要从数据源重新拉去数据，重建缓存，但是这里先不讲
        }

        return shopInfo;
    }

}
