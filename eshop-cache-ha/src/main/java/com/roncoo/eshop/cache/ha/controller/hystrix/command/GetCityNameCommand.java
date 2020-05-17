package com.roncoo.eshop.cache.ha.controller.hystrix.command;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.roncoo.eshop.cache.ha.controller.cache.local.LocationCache;

/**
 * 获取城市名称的command，使用信号量作为资源隔离策略案例
 */
public class GetCityNameCommand extends HystrixCommand<String> {

    private Long cityId;

    /**
     * 配置说明：
     * command key -> command group
     * command key -> 自己的threadpool key
     * 逻辑上来说，多个command key属于一个command group，在做统计的时候，会放在一起统计
     */
    public GetCityNameCommand(Long cityId) {
        super(
                Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("GetCityNameGroup"))  //group name
                        .andCommandKey(HystrixCommandKey.Factory.asKey("GetCityNameCommand"))  //command name
                        .andCommandPropertiesDefaults(
                                HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(10000)   //请求超时
                        )
                        .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey("GetCityNamePool"))
                        .andCommandPropertiesDefaults(
                                HystrixCommandProperties.Setter()
                                        .withExecutionIsolationStrategy(ExecutionIsolationStrategy.SEMAPHORE)   //使用信号量资源隔离策略
                                        .withExecutionIsolationSemaphoreMaxConcurrentRequests(15))              //最大并发请求数，超过则降级
        );
        this.cityId = cityId;
    }

    @Override
    protected String run() throws Exception {
        return LocationCache.getCityName(cityId);
    }

}
