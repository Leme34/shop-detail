package com.roncoo.eshop.cache.loader;

import com.roncoo.eshop.cache.task.RebuildProductCacheTask;
import com.roncoo.eshop.cache.task.RebuildShopCacheTask;
import com.roncoo.eshop.cache.zk.ZookeeperDistributedLock;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * 容器首次启动完成后，初始化zookeeper连接和缓存重建队列
 * Created by lsd
 * 2019-08-08 14:11
 */
@Component
public class InitialApplicationLoader implements ApplicationListener<ContextRefreshedEvent> {

    // 是否已启动
    private boolean alreadySetup = false;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (alreadySetup) {
            return;
        }
        ZookeeperDistributedLock.init();
        new Thread(new RebuildProductCacheTask()).start();
        new Thread(new RebuildShopCacheTask()).start();
        // 已启动
        alreadySetup = true;
    }
}
