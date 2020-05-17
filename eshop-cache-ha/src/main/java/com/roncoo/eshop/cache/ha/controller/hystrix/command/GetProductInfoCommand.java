package com.roncoo.eshop.cache.ha.controller.hystrix.command;

import com.alibaba.fastjson.JSONObject;
import com.netflix.hystrix.*;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategyDefault;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesCommandDefault;
import com.roncoo.eshop.cache.ha.controller.cache.local.BrandCache;
import com.roncoo.eshop.cache.ha.controller.cache.local.LocationCache;
import com.roncoo.eshop.cache.ha.controller.http.HttpClientUtils;
import com.roncoo.eshop.cache.ha.controller.model.ProductInfo;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

/**
 * 获取商品信息
 */
@Slf4j
public class GetProductInfoCommand extends HystrixCommand<ProductInfo> {

    private Long productId;

    // 暂存 command key 用于刷新 Hystrix 请求缓存
    private final static HystrixCommandKey GET_PRODUCT_INFO_COMMAND_KEY = HystrixCommandKey.Factory.asKey("GetProductInfoCommand");

    /**
     * 配置说明：
     * command key -> command group
     * command key -> 自己的threadpool key
     * 逻辑上来说，多个command key属于一个command group，在做统计的时候，会放在一起统计
     */
    public GetProductInfoCommand(Long productId) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("ProductInfoService"))
                .andCommandKey(GET_PRODUCT_INFO_COMMAND_KEY)
                .andCommandPropertiesDefaults(
                        HystrixCommandProperties.Setter()
                                .withExecutionTimeoutInMilliseconds(10000)      //请求超时
                                .withCircuitBreakerRequestVolumeThreshold(30)   //要求在10s内，经过短路器的流量必须达到30个，否则根本不会去判断要不要短路
                                .withCircuitBreakerErrorThresholdPercentage(40)  //当异常的访问数量达到40%时短路
                                .withCircuitBreakerSleepWindowInMilliseconds(3000)  //3s后尝试取消短路
                )
                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey("GetProductInfoPool"))
                .andThreadPoolPropertiesDefaults(
                        HystrixThreadPoolProperties.Setter()         //默认使用线程池资源隔离策略
                                .withCoreSize(10)                    //线程数
                                .withMaxQueueSize(12)                //队列大小，当线程数满了会入队列，队列满了则降级
                                .withQueueSizeRejectionThreshold(8)  //队列大小取值为 min(queueSizeRejectionThreshold,maxQueueSize)，所以此处队列大小为8
                )
        );
        this.productId = productId;
    }

    @Override
    protected ProductInfo run() throws Exception {
        // 测试一个hystrix统计时间窗口中检测到异常达到阀值时的服务熔断与恢复
        if (productId == -1L) {
            throw new IllegalArgumentException();
        }
        // 测试队列满了（超出限流）之后的服务降级（reject）
        if (productId == -2L) {
            TimeUnit.SECONDS.sleep(1);
        }

        // 测试多级降级策略
        if (productId == -3L) {
            throw new IllegalArgumentException();
        }

        String url = "http://127.0.0.1:8082/getProductInfo?productId=" + productId;
        String response = HttpClientUtils.sendGetRequest(url);
        log.info("调用商品服务服务查询商品信息，response={}", response);
        return JSONObject.parseObject(response, ProductInfo.class);
    }


    /**
     * 多级降级策略（嵌套fallback）
     */
    @Override
    protected ProductInfo getFallback() {
        return new FirstLevelFallbackCommand(productId).execute();
    }


    /**
     * command request context 的 key
     * 在一次请求上下文中，如果有多个command，调用的接口和参数都一样，则结果可以认为也是一样的，直接从command请求缓存取得并返回
     * 需要通过过滤器实现每次请求都要先调用 HystrixRequestContext.initializeContext()
     */
//    @Override
//    protected String getCacheKey() {
//        return "product_info_" + productId;
//    }

    /**
     * 清除Hystrix请求缓存
     *
     * @param productId 商品id
     */
    public static void removeCache(Long productId) {
        HystrixRequestCache.getInstance(
                GET_PRODUCT_INFO_COMMAND_KEY,
                HystrixConcurrencyStrategyDefault.getInstance()
        ).clear("product_info_" + productId);
    }

    /**
     * 第一级降级策略
     */
    private static class FirstLevelFallbackCommand extends HystrixCommand<ProductInfo> {

        private Long productId;

        public FirstLevelFallbackCommand(Long productId) {
            // 第一级的降级策略，因为这个command是运行在fallback中的
            // 所以至关重要的一点是，在做多级降级的时候，要将降级command的线程池单独做一个出来
            // TODO 如果主流程的command都失败了，可能线程池都已经被占满了，降级command必须用自己的独立的线程池
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("ProductInfoService"))
                    .andCommandKey(HystrixCommandKey.Factory.asKey("FirstLevelFallbackCommand"))
                    .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey("FirstLevelFallbackPool"))
                    .andCommandPropertiesDefaults(HystrixPropertiesCommandDefault.Setter().withExecutionTimeoutInMilliseconds(10000))
            );
            this.productId = productId;
        }

        @Override
        protected ProductInfo run() throws Exception {
            // 这里，因为是第一级降级的策略，所以说呢，其实是要从备用机房的机器去调用接口
            // 但是，我们这里没有所谓的备用机房，所以说还是调用同一个服务来模拟
            log.debug("这里是第一级降级的策略，调用同一个服务来模拟从备用机房的机器去调用接口");
            if (productId == -3L) {
                throw new IllegalArgumentException();
            }
            String url = "http://127.0.0.1:8082/getProductInfo?productId=" + productId;
            String response = HttpClientUtils.sendGetRequest(url);
            return JSONObject.parseObject(response, ProductInfo.class);
        }

        /**
         * 说明第一级降级策略失败了，执行第二级降级策略
         */
        @Override
        protected ProductInfo getFallback() {
            log.debug("第一级降级策略失败，这里是第二级降级的策略，返回本地不完整的本地缓存数据");
            return new ProductInfo()
                    .setId(productId)
                    .setBrandId(1L)
                    .setCityId(1L)
                    // 从本地缓存中获取一些数据
                    .setBrandName(BrandCache.getBrandName(productId))
                    .setCityName(LocationCache.getCityName(productId))
                    // 手动填充一些默认的数据
                    .setColor("默认颜色")
                    .setModifiedTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
                    .setName("默认商品")
                    .setPictureList("default.jpg")
                    .setPrice(0.0)
                    .setService("默认售后服务")
                    .setShopId(-1L)
                    .setSize("默认大小")
                    .setSpecification("默认规格");
        }
    }


    /**
     * ===========================================================================================================
     */


    public static void main(String[] args) throws Exception {
//        testCircuitBreaker();
//        testReject();
        testMultilevelFallback();
    }


    /**
     * 测试队列满了（超出限流）之后的服务降级（reject）
     * 我们设置线程池大小为10，队列大小为8
     * 并发25个请求（每个请求会睡眠1s），10个会进入hystrix隔离的线程池，然后8个会进入等待队列，其他7个会被reject服务降级
     * 验证方法：数一下返回结果为"降级商品"的个数==7
     */
    private static void testReject() throws Exception {
        for (int i = 0; i < 25; i++) {
            new Thread(() -> {
                String response = HttpClientUtils.sendGetRequest("http://localhost:8081/getProductInfo?productId=-2");
                log.info("请求结果为：{}", response);
            }).start();
        }
    }

    /**
     * 测试一个hystrix统计时间窗口检测到异常达到阀值时，断路器的服务熔断与恢复
     */
    private static void testCircuitBreaker() throws Exception {
        // 正常请求服务
        for (int i = 0; i < 15; i++) {
            String response = HttpClientUtils.sendGetRequest("http://localhost:8081/getProductInfo?productId=1");
            log.error("【第一个for循环】第" + (i + 1) + "次请求，结果为：" + response);
        }
        // 非法参数请求使服务不断抛异常
        for (int i = 0; i < 25; i++) {
            String response = HttpClientUtils.sendGetRequest("http://localhost:8081/getProductInfo?productId=-1");
            log.error("【第二个for循环】第" + (i + 1) + "次请求，结果为：" + response);
        }
        Thread.sleep(5000);
        // 等待了5s后，hystrix时间窗口统计发现异常比例太多，就短路了
        for (int i = 0; i < 10; i++) {
            String response = HttpClientUtils.sendGetRequest("http://localhost:8081/getProductInfo?productId=-1");
            log.error("【第三个for循环】第" + (i + 1) + "次请求，结果为：" + response);
        }
        // 统计单位，有一个时间窗口的，我们必须要等到那个时间窗口过了以后，才会说，hystrix看一下最近的这个时间窗口
        // 比如说，最近的10秒内，有多少条数据，其中异常的数据有没有到一定的比例
        // 如果到了一定的比例，那么才会去短路
        System.out.println("尝试等待3秒钟...等短路器从open转为half-open，让一条请求经过短路器");
        Thread.sleep(3000);
        for (int i = 0; i < 10; i++) {
            String response = HttpClientUtils.sendGetRequest("http://localhost:8081/getProductInfo?productId=1");
            log.error("【第四个for循环】第" + (i + 1) + "次请求，结果为：" + response);
        }
    }


    /**
     * 测试多级降级策略
     */
    private static void testMultilevelFallback() throws Exception {
        GetProductInfoCommand getProductInfoCommand1 = new GetProductInfoCommand(-1L);
        System.out.println("第一个请求的结果：" + getProductInfoCommand1.execute());
        GetProductInfoCommand getProductInfoCommand2 = new GetProductInfoCommand(-3L);
        System.out.println("第二个请求的结果：" + getProductInfoCommand2.execute());
    }

}
