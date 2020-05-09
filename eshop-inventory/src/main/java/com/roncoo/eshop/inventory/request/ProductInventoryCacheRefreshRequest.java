package com.roncoo.eshop.inventory.request;

import com.roncoo.eshop.inventory.model.ProductInventory;
import com.roncoo.eshop.inventory.service.ProductInventoryService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 当读商品库存请求未命中缓存时（可能是因为写请求先删除了缓存，也可能是数据库里压根儿没这条数据），重新加载缓存的请求
 * <p>
 * 为了减少内存队列中的请求积压(内存队列中积压的请求越多，就可能导致每个读请求自旋等待的时间越长，也可能导致多个读请求自旋等待)
 * （1）如果是因为写请求先删除了缓存的场景，则将读请求操作给压入队列中；
 * （2）如果是数据库中压根儿没这条数据的场景，那么就不应该将读请求操作给压入队列中，而是直接返回空就可以了；
 */
@Slf4j
public class ProductInventoryCacheRefreshRequest implements Request {

    //商品id
    private Integer productId;
    //商品库存Service
    private ProductInventoryService productInventoryService;
    /**
     * 是否需要强制刷新缓存，用于忽略去重
     */
    @Getter
    private boolean forceRefresh = false;


    public ProductInventoryCacheRefreshRequest(Integer productId,
                                               ProductInventoryService productInventoryService,
                                               boolean forceRefresh) {
        this.productId = productId;
        this.productInventoryService = productInventoryService;
        this.forceRefresh = forceRefresh;
    }

    @Override
    public void process() {
        // 从数据库中查询最新的商品库存数量
        ProductInventory productInventory = productInventoryService.findProductInventory(productId);
        log.debug("已从数据库中查询到最新的商品库存数量，商品id={}，库存数量={}", productInventory.getProductId(), productInventory.getInventoryCnt());
        // 将最新的商品库存数量，刷新到redis缓存中去
        productInventoryService.setProductInventoryCache(productInventory);
        log.debug("已放入redis缓存，商品id={}，库存数量={}", productInventory.getProductId(), productInventory.getInventoryCnt());
    }

    @Override
    public Integer getProductId() {
        return productId;
    }

}
