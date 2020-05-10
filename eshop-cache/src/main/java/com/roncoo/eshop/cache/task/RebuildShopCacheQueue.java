package com.roncoo.eshop.cache.task;

import com.roncoo.eshop.cache.model.ShopInfo;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * 重建店铺缓存的内存队列
 */
public class RebuildShopCacheQueue {

    private ArrayBlockingQueue<ShopInfo> queue = new ArrayBlockingQueue<>(1000);

    public void putShopInfo(ShopInfo shopInfo) {
        try {
            queue.put(shopInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ShopInfo takeShopInfo() {
        try {
            return queue.take();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 内部单例类
     */
    private static class Singleton {

        private static RebuildShopCacheQueue instance;

        static {
            instance = new RebuildShopCacheQueue();
        }

        public static RebuildShopCacheQueue getInstance() {
            return instance;
        }

    }

    public static RebuildShopCacheQueue getInstance() {
        return Singleton.getInstance();
    }

}
