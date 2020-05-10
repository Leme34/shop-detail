package com.roncoo.eshop.cache.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品信息
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProductInfo {

    private Long id;
    private String name;
    private Double price;
    private String pictureList;
    private String specification;
    private String service;
    private String color;
    private String size;
    private Long shopId;

    //缓存版本号标识，用于结合分布式锁解决并发重建缓存冲突问题
    private String modifiedTime;
}
