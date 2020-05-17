package com.roncoo.eshop.cache.ha.controller.hystrix.command;

import com.netflix.hystrix.*;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 更新商品信息
 */
@Slf4j
public class UpdateProductInfoCommand extends HystrixCommand<Boolean> {

    private Long productId;

    /**
     * 配置说明：
     * command key -> command group
     * command key -> 自己的threadpool key
     * 逻辑上来说，多个command key属于一个command group，在做统计的时候，会放在一起统计
     */
    public UpdateProductInfoCommand(Long productId) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("ProductInfoService"))
                .andCommandKey(HystrixCommandKey.Factory.asKey("UpdateProductInfoCommand"))
                .andCommandPropertiesDefaults(
                        HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(10000)  //请求超时
                )
                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey("UpdateProductInfoPool"))
                .andThreadPoolPropertiesDefaults(
                        HystrixThreadPoolProperties.Setter()         //默认使用线程池资源隔离策略
                                .withCoreSize(15)                    //线程数
                                .withQueueSizeRejectionThreshold(10) //队列大小，当线程数满了会入队列，队列满了则降级
                )
        );
        this.productId = productId;
    }

    @Override
    protected Boolean run() throws Exception {
        // 模拟调用更新商品服务
        TimeUnit.SECONDS.sleep(1);
        // 删除Hystrix缓存
        GetProductInfoCommand.removeCache(productId);
        return true;
    }

}
