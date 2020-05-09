package com.roncoo.eshop.inventory.service.impl;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import com.roncoo.eshop.inventory.request.ProductInventoryCacheRefreshRequest;
import com.roncoo.eshop.inventory.request.ProductInventoryDBUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.roncoo.eshop.inventory.request.Request;
import com.roncoo.eshop.inventory.request.RequestQueue;
import com.roncoo.eshop.inventory.service.RequestAsyncProcessService;

/**
 * 请求异步处理的service实现
 */
@Slf4j
@Service("requestAsyncProcessService")
public class RequestAsyncProcessServiceImpl implements RequestAsyncProcessService {


    @Override
    public void process(Request request) {
        try {
            Integer productId = request.getProductId();
            RequestQueue requestQueue = RequestQueue.getInstance(); //取出单例的内存队列
            // 若非强制刷新缓存，则需要检查商品库存请求入队标记，防止队列中存在同一商品的无意义请求
            if (!request.isForceRefresh()) {
                Map<Integer, Integer> flagMap = requestQueue.getProductId2FlagMap();
                if (request instanceof ProductInventoryDBUpdateRequest) {  //写请求
                    flagMap.put(productId, 0);
                } else if (request instanceof ProductInventoryCacheRefreshRequest) {  //读请求
                    // 若flag!=null且为1，说明队列已经有一个数据库更新请求+一个缓存刷新请求了，无需放入内存队列处理（去重）
                    Integer flag = flagMap.get(productId);
                    if (flag != null && flag == 1) {
                        return;
                    } else {
                        // 若flag是null，说明队列中不存在该商品库存的读/写请求，修改flag为1继续入队：flagMap.putIfAbsent(productId, 1);
                        // 若flag!=null且为0，说明队列已存在该商品库存的写请求，修改flag为1继续入队：flagMap.computeIfPresent(productId, (key, oldValue) -> oldValue == 0 ? 1 : oldValue);
                        flagMap.put(productId, 1);
                    }
                }
            }
            // 请求路由并放入队列：根据每个请求的商品id，计算对应的内存队列并入队
            this.getRoutingQueue(productId).put(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取路由目标内存队列，路由算法基于JDK1.8的HashMap的 hash()
     * <p>
     * 定位桶的算法为 hash&(length-1) ，原理是使用2的幂次掩码与hash相与，从而用hash的低位定位桶，
     * 因此仅在当前掩码上方的位发生变化的哈希集将总是相撞，
     * 所以需要位移操作来对  key.hashCode() 的高位加入扰动避免在定位桶时，低位相同而高位不同的那些key发生碰撞。
     *
     * @param productId 商品id
     * @return 内存队列
     */
    private ArrayBlockingQueue<Request> getRoutingQueue(Integer productId) {
        RequestQueue requestQueue = RequestQueue.getInstance();

        // 先获取productId的hash值
        String key = String.valueOf(productId);
        // 把hashCode右移16位与原hashCode异或，即 hashCode 的高16位与低16位异或
        int h;
        int hash = (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);

        // 相当于更高效地对hash值取模，将hash值路由到指定的内存队列中
        // 比如内存队列大小8，用内存队列的数量对hash值取模之后，结果一定是在0~7之间，所以任何一个商品id都会被路由到内存队列中的固定阻塞队列中
        int index = (requestQueue.queueSize() - 1) & hash;

        log.debug("获取路由目标内存队列，商品id={}，目标队列索引={}", productId, index);
        return requestQueue.getQueue(index);
    }

}
