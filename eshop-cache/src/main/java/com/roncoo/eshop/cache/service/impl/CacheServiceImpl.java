package com.roncoo.eshop.cache.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.roncoo.eshop.cache.model.ShopInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.roncoo.eshop.cache.model.ProductInfo;
import com.roncoo.eshop.cache.service.CacheService;
import redis.clients.jedis.JedisCluster;

/**
 * 缓存Service实现类
 * 注解 @CachePut 的 value 是 ehcache.xml 中的 cache name
 */
@Service("cacheService")
public class CacheServiceImpl implements CacheService {

    public static final String CACHE_NAME = "local";

    @Autowired
    private JedisCluster jedisCluster;

    /**
     * 将商品信息保存到本地缓存中（测试）
     */
    @CachePut(value = CACHE_NAME, key = "'key_'+#productInfo.getId()")
    public ProductInfo saveLocalCache(ProductInfo productInfo) {
        return productInfo;
    }

    /**
     * 从本地缓存中获取商品信息（测试）
     *
     * @return 先查缓存若命中则直接返回不进入方法体，若未命中缓存则走方法体的返回值
     */
    @Cacheable(value = CACHE_NAME, key = "'key_'+#id")
    public ProductInfo getLocalCache(Long id) {
        //若未命中缓存则返回此返回值
        return null;
    }



    /* ============================================================================  */


    /**
     * 将商品信息保存到本地的ehcache缓存中
     */
    @CachePut(value = CACHE_NAME, key = "'product_info_'+#productInfo.getId()")
    public ProductInfo saveProductInfo2LocalCache(ProductInfo productInfo) {
        return productInfo;
    }

    /**
     * 从本地ehcache缓存中获取商品信息
     */
    @Cacheable(value = CACHE_NAME, key = "'product_info_'+#productId")
    public ProductInfo getProductInfoFromLocalCache(Long productId) {
        return null;
    }

    /**
     * 将店铺信息保存到本地的ehcache缓存中
     */
    @CachePut(value = CACHE_NAME, key = "'shop_info_'+#shopInfo.getId()")
    public ShopInfo saveShopInfo2LocalCache(ShopInfo shopInfo) {
        return shopInfo;
    }

    /**
     * 从本地ehcache缓存中获取店铺信息
     */
    @Cacheable(value = CACHE_NAME, key = "'shop_info_'+#shopId")
    public ShopInfo getShopInfoFromLocalCache(Long shopId) {
        return null;
    }

    /**
     * 将商品信息保存到redis中
     */
    public void saveProductInfo2RedisCache(ProductInfo productInfo) {
        String key = "product_info_" + productInfo.getId();
        jedisCluster.set(key, JSONObject.toJSONString(productInfo));
    }

    /**
     * 将店铺信息保存到redis中
     */
    public void saveShopInfo2RedisCache(ShopInfo shopInfo) {
        String key = "shop_info_" + shopInfo.getId();
        jedisCluster.set(key, JSONObject.toJSONString(shopInfo));
    }


    /**
     * 从redis中获取商品信息
     */
    public ProductInfo getProductInfoFromRedisCache(Long productId) {
        String key = "product_info_" + productId;
        String json = jedisCluster.get(key);
        return JSONObject.parseObject(json, ProductInfo.class);
    }

    /**
     * 从redis中获取店铺信息
     */
    public ShopInfo getShopInfoFromRedisCache(Long shopId) {
        String key = "shop_info_" + shopId;
        String json = jedisCluster.get(key);
        return JSONObject.parseObject(json, ShopInfo.class);
    }

}
