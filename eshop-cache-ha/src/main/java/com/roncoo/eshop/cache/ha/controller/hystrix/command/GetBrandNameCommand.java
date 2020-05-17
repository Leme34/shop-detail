package com.roncoo.eshop.cache.ha.controller.hystrix.command;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.roncoo.eshop.cache.ha.controller.cache.local.BrandCache;

/**
 * 获取品牌名称的command，run方法抛异常模拟服务降级
 */
public class GetBrandNameCommand extends HystrixCommand<String> {

    private Long brandId;

    public GetBrandNameCommand(Long brandId) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("BrandInfoService"))
                .andCommandKey(HystrixCommandKey.Factory.asKey("GetBrandNameCommand"))
                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey("GetBrandInfoPool"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(10000))
                .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
                        .withCoreSize(15)
                        .withQueueSizeRejectionThreshold(10))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withFallbackIsolationSemaphoreMaxConcurrentRequests(15))
        );
        this.brandId = brandId;
    }

    @Override
    protected String run() throws Exception {
        // 调用一个品牌服务的接口
        // 如果调用失败了，报错了，那么就会去调用fallback降级机制
        throw new Exception();
    }

    @Override
    protected String getFallback() {
        System.out.println("【服务降级】从本地缓存获取过期的品牌数据，brandId=" + brandId);
        return BrandCache.getBrandName(brandId);
    }

}
