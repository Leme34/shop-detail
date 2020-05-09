package com.roncoo.eshop.inventory.controller;

import com.roncoo.eshop.inventory.model.ProductInventory;
import com.roncoo.eshop.inventory.request.ProductInventoryCacheRefreshRequest;
import com.roncoo.eshop.inventory.request.ProductInventoryDBUpdateRequest;
import com.roncoo.eshop.inventory.request.Request;
import com.roncoo.eshop.inventory.service.ProductInventoryService;
import com.roncoo.eshop.inventory.service.RequestAsyncProcessService;
import com.roncoo.eshop.inventory.vo.Response;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 商品库存
 * <p>
 * （1）一个更新商品库存的请求过来，然后此时会先删除redis中的缓存，然后模拟卡顿6s {@link ProductInventoryDBUpdateRequest#process()}
 * （2）在这个卡顿的6s内，我们发送一个商品缓存的读请求，因为此时redis中没有缓存，就会来请求将数据库中最新的数据刷新到缓存中
 * （3）此时读请求会路由到同一个内存队列中，阻塞住，不会执行
 * （4）等6s过后，写请求完成了数据库的更新之后，读请求才会执行
 * （5）读请求执行的时候，会将最新的库存从数据库中查询出来，然后更新到缓存中
 */
@Api(tags = "商品库存")
@Slf4j
@RestController
public class ProductInventoryController {

    @Resource
    private RequestAsyncProcessService requestAsyncProcessService;
    @Resource
    private ProductInventoryService productInventoryService;

    @ApiOperation("更新商品库存")
    @PostMapping("/updateProductInventory")
    public Response updateProductInventory(ProductInventory productInventory) {
        log.debug("接收到更新商品库存请求，商品id={}，库存数量={}", productInventory.getProductId(), productInventory.getInventoryCnt());
        Response response;
        try {
            // 构造数据库更新请求，并异步处理
            Request request = new ProductInventoryDBUpdateRequest(productInventory, productInventoryService);
            requestAsyncProcessService.process(request);
            response = new Response(Response.SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
            response = new Response(Response.FAILURE);
        }
        return response;
    }

    @ApiOperation("获取商品库存，保证读写操作并发时的双写一致性")
    @GetMapping("/getProductInventory")
    public ProductInventory getProductInventory(Integer productId) {
        log.debug("接收到获取商品库存的读请求，商品id={}", productId);
        ProductInventory productInventory;
        try {
            // 判断队列中是否已存在该商品库存的 重新加载库存缓存请求
            // 若没有才去构造重新加载缓存的请求，放入队列中异步处理
            requestAsyncProcessService.process(
                    new ProductInventoryCacheRefreshRequest(productId, productInventoryService, false)
            );
            // 将请求放入队列异步去处理以后，就需要自旋等待一段时间
            // 去尝试等待前面的 重新加载库存缓存 的操作
            long startTime = System.currentTimeMillis();
            long endTime;
            long waitTime = 0L;

            // 自旋读缓存200ms
            while (true) {
                if (waitTime > 200) {
                    break;
                }
                // 先尝试去redis中读取一次商品库存的缓存数据
                // 如果读取到了结果，则返回
                if ((productInventory = productInventoryService.getProductInventoryCache(productId)) != null) {
                    log.debug("自旋读取缓存成功，商品id={}，库存数量={}", productId, productInventory.getInventoryCnt());
                    return productInventory;
                } else {   // 否则没有读取到结果，阻塞等待一段时间
                    Thread.sleep(20);
                    endTime = System.currentTimeMillis();
                    waitTime = endTime - startTime;      //计算总的等待耗时
                }
            }
            log.debug("自旋超时，尝试从数据库中读取，商品id={}", productId);
            // 自旋超时，尝试从数据库中读取此时此刻的数据，若数据库都读取不到则说明不存在此商品
            if ((productInventory = productInventoryService.findProductInventory(productId)) != null) {
                log.debug("从数据库中读取成功，并追加一个强制刷新缓存的请求到队列中，商品id={}，库存数量={}", productId, productInventory.getInventoryCnt());
                // 代码会运行到这里，只有三种情况：
                // 1、就是说，上一次也是读请求，数据刷入了redis，但是redis LRU算法给清理掉了（但此时标志位还是1，会被去重，因此还需要一个标志位forceRefresh去忽略去重）
                // 所以此时下一个读请求是从缓存中拿不到数据的，因此再放一个读Request进队列，让数据去刷新一下
                // 2、可能在200ms内，就是读请求在队列中一直积压着，没有等待到它执行（在实际生产环境中，基本是比较坑了，需要优化SQL）
                // 所以就直接查一次库，然后给队列里塞进去一个刷新缓存的请求
                // 3、数据库里本身就没有，缓存穿透，穿透redis，请求到达mysql库，TODO 待解决
                requestAsyncProcessService.process(
                        new ProductInventoryCacheRefreshRequest(productId, productInventoryService, true)
                );
                return productInventory;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.debug("商品库存读取失败，可能是数据库中不存在此记录，商品id={}", productId);
        return new ProductInventory(productId, -1L);
    }

}
