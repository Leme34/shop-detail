package com.roncoo.eshop.cache.ha.controller.hystrix.collapser;

import java.util.Collection;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.netflix.hystrix.*;
import com.roncoo.eshop.cache.ha.controller.http.HttpClientUtils;
import com.roncoo.eshop.cache.ha.controller.model.ProductInfo;
import lombok.extern.slf4j.Slf4j;

/**
 * 请求合并技术（request collapser）
 * 用请求合并技术，将多个请求合并起来，可以减少高并发访问下需要使用的线程数量以及网络连接数量，这都是hystrix自动进行的
 * 其实对于高并发的查询请求来说，是可以提升性能的
 * 本质就是把设置的时间窗口内的请求（收集）合并为一个批量请求命令（批量查询）
 */
@Slf4j
public class GetProductInfosCollapser extends HystrixCollapser<List<ProductInfo>, ProductInfo, Long> {

    private Long productId;

    public GetProductInfosCollapser(Long productId) {
        super(Setter
                .withCollapserKey(HystrixCollapserKey.Factory.asKey("getProductInfosCollapser"))
                .andCollapserPropertiesDefaults(
                        HystrixCollapserProperties.Setter()
                                .withTimerDelayInMilliseconds(10)   //合并10ms内的请求
                                .withMaxRequestsInBatch(200)        //单次最多合并200个 hystrix command
                )
                .andScope(Scope.GLOBAL)
        );
        this.productId = productId;
    }

    /**
     * 一个请求收集一次请求参数
     */
    @Override
    public Long getRequestArgument() {
        return productId;
    }

    /**
     * 创建多个请求合并后的 hystrix command
     *
     * @param requests 被合并的多个请求
     */
    @Override
    protected HystrixCommand<List<ProductInfo>> createCommand(Collection<CollapsedRequest<ProductInfo, Long>> requests) {
        StringBuilder paramsBuilder = new StringBuilder("");
        for (CollapsedRequest<ProductInfo, Long> request : requests) {
            paramsBuilder.append(request.getArgument()).append(",");
        }
        String params = paramsBuilder.toString().substring(0, paramsBuilder.length() - 1); //去除最后的逗号

        System.out.println("createCommand方法执行，params=" + params);

        return new GetProductInfosCollapserBatchCommand(requests);
    }

    @Override
    protected void mapResponseToRequests(
            List<ProductInfo> batchResponse,
            Collection<CollapsedRequest<ProductInfo, Long>> requests) {
        int count = 0;
        for (CollapsedRequest<ProductInfo, Long> request : requests) {
            request.setResponse(batchResponse.get(count++));
        }
    }

    @Override
    protected String getCacheKey() {
        return "product_info_" + productId;
    }


    /**
     * 把多个请求封装在一个HystrixCommand中
     */
    private static final class GetProductInfosCollapserBatchCommand extends HystrixCommand<List<ProductInfo>> {

        public final Collection<CollapsedRequest<ProductInfo, Long>> requests;

        public GetProductInfosCollapserBatchCommand(Collection<CollapsedRequest<ProductInfo, Long>> requests) {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("ProductInfoService"))
                    .andCommandKey(HystrixCommandKey.Factory.asKey("GetProductInfosCollapserBatchCommand"))
                    .andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(10000))
            );
            this.requests = requests;
        }

        /**
         * 将多个商品id合并在一个batch内，直接发送一次网络请求，获取到所有的结果
         */
        @Override
        protected List<ProductInfo> run() throws Exception {
            // 将一个批次内的商品id给拼接在了一起
            StringBuilder paramsBuilder = new StringBuilder("");
            for (CollapsedRequest<ProductInfo, Long> request : requests) {
                paramsBuilder.append(request.getArgument()).append(",");
            }
            String params = paramsBuilder.toString().substring(0, paramsBuilder.length() - 1);

            // 将多个商品id合并在一个batch内，直接发送一次网络请求，获取到所有的结果
            String url = "http://localhost:8082/getProductInfos?productIds=" + params;
            String response = HttpClientUtils.sendGetRequest(url);

            List<ProductInfo> productInfos = JSONArray.parseArray(response, ProductInfo.class);
            productInfos.forEach(productInfo ->
                    log.info("【GetProductInfosCollapserBatchCommand】productInfo={}，isResponseFromCache={}",
                            productInfo, this.isResponseFromCache())
            );
            return productInfos;
        }

    }


    /**
     * ================================================================================================
     */
    public static void main(String[] args) throws Exception {
        String response = HttpClientUtils.sendGetRequest("http://localhost:8081/getProductInfosCollapser?productIds=1,1,2,2,3,4");
        System.out.println(response);
    }

}
