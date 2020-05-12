package com.roncoo.eshop.cache.prewarm;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.roncoo.eshop.cache.model.ProductInfo;
import com.roncoo.eshop.cache.service.CacheService;
import com.roncoo.eshop.cache.utils.SpringContextUtils;
import com.roncoo.eshop.cache.utils.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 缓存预热
 * <p>
 * 1、服务启动的时候，进行缓存预热
 * 2、从zk中读取taskid列表
 * 3、依次遍历每个taskid，尝试获取分布式锁，如果获取不到，快速报错，不要等待，因为说明已经有其他服务实例在预热了
 * 4、直接尝试获取下一个taskid的分布式锁
 * 5、即使获取到了分布式锁，也要检查一下这个taskid的预热状态，如果已经被预热过了，就不再预热了
 * 6、执行预热操作，遍历productid列表，查询数据，然后写ehcache和redis
 * 7、预热完成后，设置taskid对应的预热状态
 */
@Slf4j
public class CachePrewarmTask implements Runnable {

    @Override
    public void run() {
        CacheService cacheService = (CacheService) SpringContextUtils.getBean("cacheService");
        ZookeeperUtils zkUtils = ZookeeperUtils.getInstance();

        // 获取storm taskid列表
        String taskidList = zkUtils.getNodeData("/taskid-list");

        if (StringUtils.isNotBlank(taskidList)) {
            String[] taskidListSplited = taskidList.split(",");
            // 遍历取出每个task统计出的热门商品列表
            for (String taskid : taskidListSplited) {
                // 1 锁定这个task的热门商品列表
                String taskidLockPath = "/taskid-lock-" + taskid;
                boolean lock = zkUtils.tryAcquireDistributedLock(taskidLockPath);
                if (!lock) {
                    continue;
                }
                try {
                    // 2 锁定此task的预热状态，防止数据被重复预热
                    String taskidStatusLockPath = "/taskid-status-lock-" + taskid;
                    zkUtils.acquireDistributedLock(taskidStatusLockPath);
                    try {
                        // 此task的热门商品列表是否已被预热的标记节点，若已被预热过，则跳过
                        String isThisTaskLoadedPath = "/taskid-status-" + taskid;
                        zkUtils.createNode(isThisTaskLoadedPath);
                        String taskidStatus = zkUtils.getNodeData(isThisTaskLoadedPath);
                        if (!StringUtils.isEmpty(taskidStatus)) {
                            log.debug("热门商品列表已被预热过，跳过，taskid={}，taskidStatus={}", taskid, taskidStatus);
                            continue;
                        }
                        // 预热逻辑（加载到Redis与本地ehcache缓存）
                        String productidList = zkUtils.getNodeData("/task-hot-product-list-" + taskid);
                        JSONArray productidJSONArray = JSONArray.parseArray(productidList);
                        log.debug("获取到热门商品列表，开始预热，taskid={}，productidList={}", taskid, productidList);
                        for (int i = 0; i < productidJSONArray.size(); i++) {
                            Long productId = productidJSONArray.getLong(i);
                            // 模拟调用服务查询数据库的返回结果
                            String productInfoJSON = "{\"id\": " + productId + ", \"name\": \"iphone7手机\", \"price\": 5599, \"pictureList\":\"a.jpg,b.jpg\", \"specification\": \"iphone7的规格\", \"service\": \"iphone7的售后服务\", \"color\": \"红色,白色,黑色\", \"size\": \"5.5\", \"shopId\": 1, \"modifiedTime\": \"2017-01-01 12:00:00\"}";
                            ProductInfo productInfo = JSONObject.parseObject(productInfoJSON, ProductInfo.class);
                            cacheService.saveProductInfo2LocalCache(productInfo);
                            cacheService.saveProductInfo2RedisCache(productInfo);
                        }
                        zkUtils.setNodeData(isThisTaskLoadedPath, "success");
                    } finally {
                        zkUtils.releaseDistributedLock(taskidStatusLockPath);
                    }
                } finally {
                    zkUtils.releaseDistributedLock(taskidLockPath);
                }
            }
        }
    }

}
