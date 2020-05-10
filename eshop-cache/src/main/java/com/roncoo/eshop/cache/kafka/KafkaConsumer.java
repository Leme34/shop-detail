package com.roncoo.eshop.cache.kafka;

import com.alibaba.fastjson.JSONObject;
import com.roncoo.eshop.cache.model.ProductInfo;
import com.roncoo.eshop.cache.model.ShopInfo;
import com.roncoo.eshop.cache.service.CacheService;
import com.roncoo.eshop.cache.zk.ZookeeperDistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Kafka消息监听器
 * <p>
 * 异步构建缓存测试接口见 {@link com.roncoo.eshop.cache.controller.KafkaController}
 * <p>
 * Created by lsd
 * 2020-05-08 22:13
 */
@Slf4j
@Component
public class KafkaConsumer {

    public final static String CACHE_TOPIC = "cache-message";
    private final static ThreadLocal<SimpleDateFormat> DATE_TIME_FORMATTER = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    );

    @Resource
    private CacheService cacheService;

    @KafkaListener(topics = CACHE_TOPIC)
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
        // modifiedTime字段是缓存版本号标识，用于结合分布式锁解决并发重建缓存冲突问题
        String productInfoJSON = "{\"id\": 2, \"name\": \"iphone7手机\", \"price\": 5599, \"pictureList\":\"a.jpg,b.jpg\", \"specification\": \"iphone7的规格\", \"service\": \"iphone7的售后服务\", \"color\": \"红色,白色,黑色\", \"size\": \"5.5\", \"shopId\": 1, \"modifiedTime\": \"2017-01-01 12:00:00\"}";
        ProductInfo productInfo = JSONObject.parseObject(productInfoJSON, ProductInfo.class);
        cacheService.saveProductInfo2LocalCache(productInfo);
        log.debug("商品信息已保存到本地ehcache缓存，productId={}", productId);

        // 先获取分布式锁，再比对缓存版本号，若是最新数据才放入Redis缓存
        this.saveProductInfoRedisCache(productInfo);
    }


    /**
     * 先获取分布式锁，再比对缓存版本号，若是最新数据才放入Redis缓存
     *
     * @param productInfo 商品信息
     */
    private void saveProductInfoRedisCache(ProductInfo productInfo) {
        Long productId = productInfo.getId();
        ZookeeperDistributedLock distributedLock = ZookeeperDistributedLock.getInstance();
        try {
            distributedLock.acquireDistributedLock(productId, 0);
            // 比较当前数据的时间版本比已有数据的时间版本是新还是旧
            ProductInfo oldProductInfo = cacheService.getProductInfoFromRedisCache(productId);
            if (oldProductInfo != null && oldProductInfo.getModifiedTime() != null) {
                SimpleDateFormat sdf = DATE_TIME_FORMATTER.get();
                Date date = sdf.parse(productInfo.getModifiedTime());
                Date oldDate = sdf.parse(oldProductInfo.getModifiedTime());
                if (date.before(oldDate)) {
                    log.debug("缓存版本号比对结果：current date[{}] is before existed date[{}]，不更新Redis缓存", productInfo.getModifiedTime(), oldDate);
                    return;
                }
                log.debug("缓存版本号比对结果：current date[{}] is after existed date[{}]，更新Redis缓存", productInfo.getModifiedTime(), oldDate);
            }
            // 放入Redis缓存
            cacheService.saveProductInfo2RedisCache(productInfo);
            log.debug("商品信息已保存到Redis，productId={}", productId);
        } catch (Exception e) {
            log.error("商品信息保存到Redis失败", e);
        } finally {
            distributedLock.releaseDistributedLock(productId, 0);
        }
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
        String shopInfoJSON = "{\"id\": 2, \"name\": \"老王的手机店\", \"level\": 5, \"goodCommentRate\":0.99}, \"modifiedTime\": \"2017-01-01 12:00:00\"}";
        ShopInfo shopInfo = JSONObject.parseObject(shopInfoJSON, ShopInfo.class);
        cacheService.saveShopInfo2LocalCache(shopInfo);
        log.debug("店铺信息已保存到本地ehcache缓存，shopId={}", shopId);
        // 先获取分布式锁，再比对缓存版本号，若是最新数据才放入Redis缓存
        this.saveShopInfoRedisCache(shopInfo);
    }


    /**
     * 先获取分布式锁，再比对缓存版本号，若是最新数据才放入Redis缓存
     *
     * @param shopInfo 店铺信息
     */
    private void saveShopInfoRedisCache(ShopInfo shopInfo) {
        Long shopId = shopInfo.getId();
        ZookeeperDistributedLock distributedLock = ZookeeperDistributedLock.getInstance();
        try {
            distributedLock.acquireDistributedLock(shopId, 1);
            // 比较当前数据的时间版本比已有数据的时间版本是新还是旧
            ShopInfo oldShopInfo = cacheService.getShopInfoFromRedisCache(shopId);
            if (oldShopInfo != null && oldShopInfo.getModifiedTime() != null) {
                SimpleDateFormat sdf = DATE_TIME_FORMATTER.get();
                Date date = sdf.parse(shopInfo.getModifiedTime());
                Date oldDate = sdf.parse(oldShopInfo.getModifiedTime());
                if (date.before(oldDate)) {
                    log.debug("缓存版本号比对结果：current date[{}] is before existed date[{}]，不更新Redis缓存", shopInfo.getModifiedTime(), oldDate);
                    return;
                }
                log.debug("缓存版本号比对结果：current date[{}] is after existed date[{}]，更新Redis缓存", shopInfo.getModifiedTime(), oldDate);
            }
            // 放入Redis缓存
            cacheService.saveShopInfo2RedisCache(shopInfo);
            log.debug("店铺信息已保存到Redis，shopId={}", shopId);
        } catch (Exception e) {
            log.error("店铺信息保存到Redis失败", e);
        } finally {
            distributedLock.releaseDistributedLock(shopId, 1);
        }
    }

}
