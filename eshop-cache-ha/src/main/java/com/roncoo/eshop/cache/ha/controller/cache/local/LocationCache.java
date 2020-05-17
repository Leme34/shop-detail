package com.roncoo.eshop.cache.ha.controller.cache.local;

import java.util.HashMap;
import java.util.Map;

/**
 * 模拟本地缓存
 */
public class LocationCache {

    private final static Map<Long, String> cityMap = new HashMap<>();

    static {
        cityMap.put(1L, "北京");
    }

    public static String getCityName(Long cityId) {
        return cityMap.get(cityId);
    }

}
