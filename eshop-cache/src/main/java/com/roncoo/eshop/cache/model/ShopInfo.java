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

	//缓存版本号标识，用于结合分布式锁解决并发重建缓存冲突问题
	private String modifiedTime;
}
