package com.roncoo.eshop.cache.model;

import lombok.Data;

/**
 * 店铺信息
 */
@Data
public class ShopInfo {

	private Long id;
	private String name;
	private Integer level;
	private Double goodCommentRate;

}
