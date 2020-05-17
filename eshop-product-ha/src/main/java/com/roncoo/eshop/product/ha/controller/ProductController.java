package com.roncoo.eshop.product.ha.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品服务的接口
 */
@RestController
public class ProductController {

    /**
     * 模拟查询单个
     */
    @RequestMapping("/getProductInfo")
    public String getProductInfo(Long productId) {
        // 模拟查询数据库
        return "{\"id\": " + productId + ", \"name\": \"iphone7手机\", \"price\": 5599, \"pictureList\":\"a.jpg,b.jpg\", \"specification\": \"iphone7的规格\", \"service\": \"iphone7的售后服务\", \"color\": \"红色,白色,黑色\", \"size\": \"5.5\", \"shopId\": 1, \"modifiedTime\": \"2017-01-01 12:00:00\", \"cityId\": 1, \"brandId\": 1}";
    }

    /**
     * 模拟批量查询
     */
    @RequestMapping("/getProductInfos")
    public String getProductInfos(String productIds) {
        System.out.println("getProductInfos接口，接收到一次请求，productIds=" + productIds);
        JSONArray jsonArray = new JSONArray();
        for (String productId : productIds.split(",")) {
            String json = "{\"id\": " + productId + ", \"name\": \"iphone7手机\", \"price\": 5599, \"pictureList\":\"a.jpg,b.jpg\", \"specification\": \"iphone7的规格\", \"service\": \"iphone7的售后服务\", \"color\": \"红色,白色,黑色\", \"size\": \"5.5\", \"shopId\": 1, \"modifiedTime\": \"2017-01-01 12:00:00\", \"cityId\": 1, \"brandId\": 1}";
            jsonArray.add(JSONObject.parseObject(json));
        }
        return jsonArray.toJSONString();
    }

}
