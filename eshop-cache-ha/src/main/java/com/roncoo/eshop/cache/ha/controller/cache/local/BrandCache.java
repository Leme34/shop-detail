package com.roncoo.eshop.cache.ha.controller.cache.local;

import java.util.HashMap;
import java.util.Map;

/**
 * 品牌缓存
 */
public class BrandCache {

	private final static Map<Long, String> brandMap = new HashMap<>();

	static {
		brandMap.put(1L, "iphone");
	}

	public static String getBrandName(Long brandId) {
		return brandMap.get(brandId);
	}

}
