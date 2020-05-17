package com.roncoo.eshop.cache.ha.controller;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixObservableCommand;
import com.roncoo.eshop.cache.ha.controller.http.HttpClientUtils;
import com.roncoo.eshop.cache.ha.controller.hystrix.collapser.GetProductInfosCollapser;
import com.roncoo.eshop.cache.ha.controller.hystrix.command.GetBrandNameCommand;
import com.roncoo.eshop.cache.ha.controller.hystrix.command.GetCityNameCommand;
import com.roncoo.eshop.cache.ha.controller.hystrix.command.GetProductInfoCommand;
import com.roncoo.eshop.cache.ha.controller.hystrix.observableCommand.GetProductInfosCommand;
import com.roncoo.eshop.cache.ha.controller.model.ProductInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rx.Observable;
import rx.Observer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;


/**
 * 缓存服务的接口
 */
@Slf4j
@RestController
public class CacheController {

    @RequestMapping("/change/product")
    public String changeProduct(Long productId) {
        // 拿到一个商品id
        // 调用商品服务的接口，获取商品id对应的商品的最新数据
        // 用HttpClient去调用商品服务的http接口
        String url = "http://127.0.0.1:8082/getProductInfo?productId=" + productId;
        String response = HttpClientUtils.sendGetRequest(url);
        System.out.println(response);

        return "success";
    }


    /**
     * 若从nginx开始到各级缓存都失效了，则需要从该接口拉取数据
     * http://localhost:8081/getProductInfo?productId=1
     */
    @RequestMapping("/getProductInfo")
    public ProductInfo getProductInfo(Long productId) {
        // 拿到一个商品id调用商品服务的接口，获取商品id对应的商品的最新数据，用HttpClient去调用商品服务的http接口
        // 把以上逻辑封装在 HystrixCommand 中，实现资源隔离
        HystrixCommand<ProductInfo> getProductInfoCommand = new GetProductInfoCommand(productId);
        ProductInfo productInfo = getProductInfoCommand.execute();

        // 使用信号量作为资源隔离策略
        GetCityNameCommand getCityNameCommand = new GetCityNameCommand(productInfo.getCityId());
        String cityName = getCityNameCommand.execute();
        productInfo.setCityName(cityName);

        // 服务降级
        Long brandId = productInfo.getBrandId();
        GetBrandNameCommand getBrandNameCommand = new GetBrandNameCommand(brandId);
        String brandName = getBrandNameCommand.execute();
        productInfo.setBrandName(brandName);

        // 异步执行HystrixCommand
//		Future<ProductInfo> future = getProductInfoCommand.queue();
//		try {
//			Thread.sleep(1000);
//			System.out.println(future.get());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

        System.out.println(productInfo);
        return productInfo;
    }


    /**
     * 批量查询多条商品数据的请求【非hystrix请求缓存】
     * http://localhost:8081/getProductInfos?productIds=1,2,3
     */
    @RequestMapping("/getProductInfos")
    public String getProductInfos(String productIds) {
        // 把多个查询请求封装在 HystrixObservableCommand 中，实现资源隔离
        HystrixObservableCommand<ProductInfo> getProductInfosCommand =
                new GetProductInfosCommand(productIds.split(","));
        // 立即执行
        Observable<ProductInfo> observable = getProductInfosCommand.observe();

        // 延迟执调用后未开始执行，HystrixObservableCommand.construct() 方法
//		observable = getProductInfosCommand.toObservable();

        // 监听 HystrixObservableCommand 中的回调
        observable.subscribe(new Observer<ProductInfo>() {
            public void onCompleted() {
                log.info("获取完了所有的商品数据");
            }

            public void onError(Throwable e) {
                log.error("查询商品信息服务调用出错", e);
            }

            /**
             * HystrixObservableCommand中的每个查询请求的返回时通过 observer.onNext 回调
             */
            public void onNext(ProductInfo productInfo) {
                log.info("productInfo={}", productInfo);
            }
        });

        return "success";
    }

    /**
     * 批量查询多条商品数据的请求【使用hystrix请求缓存】
     * http://localhost:8081/getProductInfosCache?productIds=1,1,2,3,3
     * 同一个command中使用同样参数第二次请求时可以看到 isResponseFromCache=true
     */
    @RequestMapping("/getProductInfosCache")
    public String getProductInfosCache(String productIds) {
        for (String productId : productIds.split(",")) {
            GetProductInfoCommand getProductInfoCommand = new GetProductInfoCommand(Long.valueOf(productId));
            ProductInfo productInfo = getProductInfoCommand.execute();  //相当于Future.get()，然后阻塞等待返回
            log.info("productInfo={}，isResponseFromCache={}", productInfo, getProductInfoCommand.isResponseFromCache());
        }
        return "success";
    }


    /**
     * 批量查询多条商品数据的请求【使用hystrix请求合并技术将多个command请求合并到一个command中执行，减少你对商品服务访问时的网络I/O开销】
     * 测试：{@link GetProductInfosCollapser#main}
     */
    @RequestMapping("/getProductInfosCollapser")
    public String getProductInfosCollapser(String productIds) {
        List<Future<ProductInfo>> futures = new ArrayList<>();
        for (String productId : productIds.split(",")) {
            GetProductInfosCollapser getProductInfosCollapser =
                    new GetProductInfosCollapser(Long.valueOf(productId));
            futures.add(getProductInfosCollapser.queue());  //queue()会返回future
        }
        try {
            for (Future<ProductInfo> future : futures) {
                log.debug("使用hystrix请求合并技术返回的结果：{}", future.get());
            }
        } catch (Exception e) {
            log.error("使用hystrix请求合并技术批量查询出错", e);
        }
        return "success";
    }

}
