package com.roncoo.eshop.inventory.request;

/**
 * 请求接口
 *
 */
public interface Request {

    /**
     * 请求处理逻辑
     */
    void process();

    /**
     * 获取商品ID
     */
    Integer getProductId();

    /**
     * 是否强制刷新缓存（忽略去重）
     */
    boolean isForceRefresh();

}
