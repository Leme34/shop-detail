package com.roncoo.eshop.cache.task;

import com.roncoo.eshop.cache.model.ShopInfo;
import com.roncoo.eshop.cache.service.CacheService;
import com.roncoo.eshop.cache.utils.SpringContextUtils;
import com.roncoo.eshop.cache.utils.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 店铺缓存重建任务
 */
@Slf4j
public class RebuildShopCacheTask implements Runnable {

    private final static ThreadLocal<SimpleDateFormat> DATE_TIME_FORMATTER = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    );

    public void run() {
        RebuildShopCacheQueue rebuildShopCacheQueue = RebuildShopCacheQueue.getInstance();
        ZookeeperUtils zkUtils = ZookeeperUtils.getInstance();
        CacheService cacheService = (CacheService) SpringContextUtils.getBean("cacheService");

        // 一直消费队列中的请求
        while (true) {
            ShopInfo shopInfo = rebuildShopCacheQueue.takeShopInfo();
            Long shopId = shopInfo.getId();
            try {
                // 先获取分布式锁，再比对缓存版本号，若是最新数据才放入Redis缓存
                zkUtils.acquireDistributedLock(this.getLockPath(shopId));
                ShopInfo oldShopInfo = cacheService.getShopInfoFromRedisCache(shopId);

                // 比较当前数据的时间版本比已有数据的时间版本是新还是旧
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

                // 放入本地ehcache缓存
                cacheService.saveShopInfo2LocalCache(shopInfo);
                log.debug("店铺信息已保存到本地ehcache缓存，shopId={}", shopId);
            } catch (Exception e) {
                log.error("店铺信息缓存重建失败", e);
            } finally {
                zkUtils.releaseDistributedLock(this.getLockPath(shopInfo.getId()));
            }
        }
    }


    /**
     * 获取分布式锁的锁定目录
     *
     * @param id 店铺id
     */
    private String getLockPath(Long id) {
        return "/shop-lock-" + id;
    }

}
