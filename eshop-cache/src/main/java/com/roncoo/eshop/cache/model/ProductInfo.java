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

}
