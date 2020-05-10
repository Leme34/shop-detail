package com.roncoo.eshop.inventory.listener;

import com.roncoo.eshop.inventory.requestprocessor.RequestProcessorThreadPool;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * 容器首次启动完成后，初始化工作线程池和内存队列
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
        // 初始化工作线程池和内存队列
        RequestProcessorThreadPool.init();
        // 已启动
        alreadySetup = true;
    }
}
