package com.roncoo.eshop.cache.task;

import com.roncoo.eshop.cache.model.ProductInfo;
import com.roncoo.eshop.cache.service.CacheService;
import com.roncoo.eshop.cache.utils.SpringContextUtils;
import com.roncoo.eshop.cache.utils.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 商品缓存重建任务
 */
@Slf4j
public class RebuildProductCacheTask implements Runnable {

    private final static ThreadLocal<SimpleDateFormat> DATE_TIME_FORMATTER = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    );

    public void run() {
        RebuildProductCacheQueue rebuildProductCacheQueue = RebuildProductCacheQueue.getInstance();
        ZookeeperUtils zkUtils = ZookeeperUtils.getInstance();
        CacheService cacheService = (CacheService) SpringContextUtils.getBean("cacheService");

        // 一直消费队列中的请求
        while (true) {
            ProductInfo productInfo = rebuildProductCacheQueue.takeProductInfo();
            Long productId = productInfo.getId();
            try {
                // 先获取分布式锁，再比对缓存版本号，若是最新数据才放入Redis缓存
                zkUtils.acquireDistributedLock(this.getLockPath(productId));
                ProductInfo oldProductInfo = cacheService.getProductInfoFromRedisCache(productId);

                // 比较当前数据的时间版本比已有数据的时间版本是新还是旧
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

                // 放入本地ehcache缓存
                cacheService.saveProductInfo2LocalCache(productInfo);
                log.debug("商品信息已保存到本地ehcache缓存，productId={}", productId);
            } catch (Exception e) {
                log.error("商品信息缓存重建失败", e);
            } finally {
                zkUtils.releaseDistributedLock(this.getLockPath(productInfo.getId()));
            }
        }
    }


    /**
     * 获取分布式锁的锁定目录
     *
     * @param id 商品id
     */
    private String getLockPath(Long id) {
        return "/product-lock-" + id;
    }

}
