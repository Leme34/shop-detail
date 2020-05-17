package com.roncoo.eshop.cache.ha.controller.hystrix.observableCommand;

import com.netflix.hystrix.HystrixCommandProperties;
import com.roncoo.eshop.cache.ha.controller.http.HttpClientUtils;
import com.roncoo.eshop.cache.ha.controller.model.ProductInfo;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import com.alibaba.fastjson.JSONObject;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;

/**
 * 批量查询多个商品数据的command
 * HystrixCommand主要用于仅仅会返回一个结果的调用
 * HystrixObservableCommand主要用于可能会返回多条结果的调用
 */
public class GetProductInfosCommand extends HystrixObservableCommand<ProductInfo> {

    private String[] productIds;

    /**
     * 构造方法
     *
     * @param productIds
     */
    public GetProductInfosCommand(String[] productIds) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("GetProductInfoGroup"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(10000))
        );
        this.productIds = productIds;
    }

    @Override
    protected Observable<ProductInfo> construct() {
        return Observable.create((Observable.OnSubscribe<ProductInfo>) observer -> {
            try {
                // 批量调用查询商品信息的服务
                for (String productId : productIds) {
                    String url = "http://127.0.0.1:8082/getProductInfo?productId=" + productId;
                    String response = HttpClientUtils.sendGetRequest(url);
                    ProductInfo productInfo = JSONObject.parseObject(response, ProductInfo.class);
                    // 调用 onNext 回调(钩子)方法
                    observer.onNext(productInfo);
                }
                observer.onCompleted();
            } catch (Exception e) {
                observer.onError(e);
            }
        }).subscribeOn(Schedulers.io());
    }

}
