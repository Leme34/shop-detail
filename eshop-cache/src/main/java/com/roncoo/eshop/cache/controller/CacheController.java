package com.roncoo.eshop.cache.controller;

import com.alibaba.fastjson.JSONObject;
import com.roncoo.eshop.cache.hystrix.command.GetProductInfoCommand;
import com.roncoo.eshop.cache.model.ProductInfo;
import com.roncoo.eshop.cache.model.ShopInfo;
import com.roncoo.eshop.cache.prewarm.CachePrewarmTask;
import com.roncoo.eshop.cache.service.CacheService;
import com.roncoo.eshop.cache.task.RebuildProductCacheQueue;
import com.roncoo.eshop.cache.task.RebuildShopCacheQueue;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 商品详情页面url：http://192.168.11.102/product?requestPath=product&productId=1&shopId=1
 */
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
        log.debug("从redis中获取到的商品信息={}", productInfo);

        // 2 未命中Redis缓存，再查询本地ehcache缓存
        if (productInfo == null) {
            productInfo = cacheService.getProductInfoFromLocalCache(productId);
            log.debug("从ehcache中获取到的商品信息={}", productInfo);
        }

        // 3 若缓存都未命中，则需要从数据库重新读数据，并重建缓存
        if (productInfo == null) {
            // 模拟数据库查询返回的结果
            new GetProductInfoCommand(productId).execute();
            // 放入缓存重建队列
            RebuildProductCacheQueue.getInstance().putProductInfo(productInfo);
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
        log.debug("从redis中获取到的店铺信息={}", shopInfo);

        // 2 未命中Redis缓存，再查询本地ehcache缓存
        if (shopInfo == null) {
            shopInfo = cacheService.getShopInfoFromLocalCache(shopId);
            log.debug("从ehcache中获取到的店铺信息={}", shopInfo);
        }

        // 3 若缓存都未命中，则需要从数据库重新读数据，并重建缓存
        if (shopInfo == null) {
            // 模拟数据库查询返回的结果
            String shopInfoJSON = "{\"id\": " + shopId + ", \"name\": \"老王的手机店\", \"level\": 5, \"goodCommentRate\":0.99, \"modifiedTime\": \"2017-01-01 12:00:00\"}";
            shopInfo = JSONObject.parseObject(shopInfoJSON, ShopInfo.class);
            // 放入缓存重建队列
            RebuildShopCacheQueue.getInstance().putShopInfo(shopInfo);
            log.debug("缓存都未命中，重建缓存，从数据库重新读取的店铺信息={}", shopInfo);
        }
        return shopInfo;
    }


    @ApiOperation(value = "启动缓存数据预热线程", notes = "预热每个storm task统计出来的topN热门数据")
    @GetMapping("/prewarm")
    public String prewarmCache() {
        // 真实环境中应使用线程池
        new Thread(new CachePrewarmTask()).start();
        return "success";
    }

}
