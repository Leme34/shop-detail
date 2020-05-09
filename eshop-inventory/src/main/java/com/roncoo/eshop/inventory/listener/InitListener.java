package com.roncoo.eshop.inventory.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.roncoo.eshop.inventory.requestprocessor.RequestProcessorThreadPool;

/**
 * Web应用生命周期监听器
 */
public class InitListener implements ServletContextListener {

    /**
     * 当启动 Web 应用时调用该方法。
	 * 在调用完该方法之后，容器再对 Filter 初始化，
     * 并且对那些在 Web 应用启动时就需要被初始化的 Servlet 进行初始化。
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 初始化工作线程池和内存队列
        RequestProcessorThreadPool.init();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

}
