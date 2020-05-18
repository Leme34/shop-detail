package com.roncoo.eshop.cache.hystrix.command;

import com.alibaba.fastjson.JSONObject;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.roncoo.eshop.cache.model.ProductInfo;
import com.roncoo.eshop.cache.utils.SpringContextUtils;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.JedisCluster;

public class GetProductInfoFromRedisCacheCommand extends HystrixCommand<ProductInfo> {

    private Long productId;

    public GetProductInfoFromRedisCacheCommand(Long productId) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("RedisGroup"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionTimeoutInMilliseconds(100)          //超时时长
                        .withCircuitBreakerRequestVolumeThreshold(1000)   //设置一个滑动窗口(rolling window)中，最少要有多少个请求才启用熔断器
                        .withCircuitBreakerErrorThresholdPercentage(70)   //设置异常请求量的百分比，当异常请求达到这个百分比时触发断路
                        .withCircuitBreakerSleepWindowInMilliseconds(60 * 1000))  //设置在断路之后，需要在多长时间内直接reject请求，然后在这段时间之后再重新变为half-open状态，尝试允许请求通过以及自动恢复
        );
        this.productId = productId;
    }

    @Override
    protected ProductInfo run() throws Exception {
        JedisCluster jedisCluster = (JedisCluster) SpringContextUtils.getBean("JedisClusterFactory");
        String key = "product_info_" + productId;
        String json = jedisCluster.get(key);
        return (StringUtils.isBlank(json) || StringUtils.equalsIgnoreCase(json, "null")) ? null :
                JSONObject.parseObject(json, ProductInfo.class);
    }

}
