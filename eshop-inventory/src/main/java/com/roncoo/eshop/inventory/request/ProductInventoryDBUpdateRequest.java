package com.roncoo.eshop.inventory.request;

import com.roncoo.eshop.inventory.model.ProductInventory;
import com.roncoo.eshop.inventory.service.ProductInventoryService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 数据库更新请求，比如说一个商品发生了交易，那么就要修改这个商品对应的库存
 * cache aside pattern
 * （1）先删除缓存
 * （2）再更新数据库
 */
@Slf4j
public class ProductInventoryDBUpdateRequest implements Request {

    /**
     * 商品库存
     */
    private ProductInventory productInventory;
    /**
     * 商品库存Service
     */
    private ProductInventoryService productInventoryService;

    public ProductInventoryDBUpdateRequest(ProductInventory productInventory,
                                           ProductInventoryService productInventoryService) {
        this.productInventory = productInventory;
        this.productInventoryService = productInventoryService;
    }

    @Override
    public void process() {
        log.debug("数据库更新请求开始执行，商品id={}，库存数量={}", productInventory.getProductId(), productInventory.getInventoryCnt());
        // 删除redis中的缓存
        productInventoryService.removeProductInventoryCache(productInventory);
        log.debug("已删除redis中的缓存，商品id={}，库存数量={}", productInventory.getProductId(), productInventory.getInventoryCnt());
        // 为了模拟删除缓存后写数据库时处理其他线程的读请求是否会造成数据库缓存不一致，此处睡眠一段时间
        try {
            TimeUnit.SECONDS.sleep(6);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 修改数据库中的库存
        productInventoryService.updateProductInventory(productInventory);
        log.debug("已更新数据库中的库存，商品id={}，库存数量={}", productInventory.getProductId(), productInventory.getInventoryCnt());
    }

    @Override
    public Integer getProductId() {
        return productInventory.getProductId();
    }

    /**
     * 数据库更新请求无需去重（此参数对 数据库更新请求 无意义）
     */
    @Override
    public boolean isForceRefresh() {
        return true;
    }

}
