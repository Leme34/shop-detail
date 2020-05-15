package com.roncoo.eshop.storm.bolt;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.roncoo.eshop.storm.http.HttpClientUtils;
import com.roncoo.eshop.storm.zk.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.trident.util.LRUMap;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.utils.Utils;

/**
 * 商品访问次数统计bolt，上游是Nginx日志解析的bolt {@link com.roncoo.eshop.storm.bolt.LogParseBolt}
 */
@Slf4j
public class ProductCountBolt extends BaseRichBolt {

    private static final long serialVersionUID = -8761807561458126413L;

    // 存放访问次数统计结果<productId,访问次数>
    private LRUMap<Long, Long> productCountMap = new LRUMap<>(1000);

    private ZookeeperUtils zkUtils;
    private int taskid;


    /**
     * 对于bolt来说，第一个方法，就是prepare方法
     * <p>
     * OutputCollector，这个也是Bolt的这个tuple的发射器
     */
    public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
        // 启动topN结果集计算线程
        new Thread(new ProductCountThread()).start();
        // 启动热点发现线程
        new Thread(new HotProductFindThread()).start();
        // 初始化zk连接
        this.zkUtils = ZookeeperUtils.getInstance();
        // 1、将自己的taskid追加到一个zookeeper node中，形成taskid的列表(格式就是逗号分隔，拼接成一个列表)
        // 2、然后每次都将自己的热门商品列表，写入自己的taskid对应的zookeeper节点
        // 3、然后这样的话，并行的预热程序才能从taskid列表中知道有哪些taskid
        // 4、然后并行预热程序根据每个taskid去获取一个锁，然后再从对应的zk node中拿到热门商品列表
        this.addTaskId(context.getThisTaskId());
        this.taskid = context.getThisTaskId();
    }

    /**
     * execute方法
     * <p>
     * 就是说，每次接收到一条数据后，就会交给这个executor方法来执行
     */
    public void execute(Tuple tuple) {
        Long productId = tuple.getLongByField("productId");
        log.info("接收到一个商品id={}", productId);

        Long count = productCountMap.get(productId);
        if (count == null) {
            count = 0L;
        }
        count++;
        productCountMap.put(productId, count);
        log.info("商品访问计数+1，productId={}，count={}", productId, count);
    }

    /**
     * 定义发射出去的tuple，每个field的名称
     */
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
    }


    /**
     * ProductCountBolt所有的task启动的时候， 都会将自己的taskid写到同一个专用于记录taskid列表的 zk node 中
     * 格式就是逗号分隔，拼接成一个taskid列表
     *
     * @param taskid storm任务id
     */
    private void addTaskId(int taskid) {
        zkUtils.acquireDistributedLock(getLockPath());
        try {
            zkUtils.createNode("/taskid-list");  // 读/写节点数据前都必须先创建出节点
            String taskidList = zkUtils.getNodeData("/taskid-list");
            //非空则以逗号分隔拼接
            taskidList = StringUtils.isNotBlank(taskidList) ? taskidList + "," + taskid : String.valueOf(taskid);
            zkUtils.setNodeData("/taskid-list", taskidList);
            log.info("新加入taskid={}，当前taskidList={}", taskid, taskidList);
        } finally {
            zkUtils.releaseDistributedLock(getLockPath());
        }
    }

    /**
     * 每分钟计算1次topN结果集存入zk，提供给冷启动时的缓存预热接口使用
     */
    private class ProductCountThread implements Runnable {
        @Override
        public void run() {
            log.info("topN计算线程正在运行...");
            // 当前LRUMap的访问次数排序后的topN结果集
            List<Map.Entry<Long, Long>> topNProductList = new ArrayList<>();
            while (true) {
                try {
                    topNProductList.clear();    //每次重新计算topN结果集
                    int n = 3;

                    if (productCountMap.size() == 0) {  //若LRUMap为空则100ms后再计算
                        Utils.sleep(100);
                        continue;
                    }

                    // 1 遍历存放访问次数统计结果的LRUMap，并(排名)筛选出放入topN结果集的元素
                    for (Map.Entry<Long, Long> productCountEntry : productCountMap.entrySet()) {
                        // 若topN结果集为空则直接放入
                        if (topNProductList.size() == 0) {
                            topNProductList.add(productCountEntry);
                        } else {   //否则需要与topN结果集已有的数据做比较
                            boolean bigger = false;
                            // 2 遍历当前topN结果集
                            for (int i = 0; i < topNProductList.size(); i++) {
                                // 若LRUMap的当前元素 > topN结果集的当前元素，则需要把topN结果集lastIndex以后的所有元素往后移1位(腾出空位)，再放入topN结果集
                                if (productCountEntry.getValue() > topNProductList.get(i).getValue()) {
                                    int lastIndex = topNProductList.size() < n ? topNProductList.size() - 1 : n - 2; // 最后一个元素的下标
                                    for (int j = lastIndex; j >= i; j--) {
                                        if (j + 1 == topNProductList.size()) {  //越界情况处理：添加空元素触发扩容
                                            topNProductList.add(null);
                                        }
                                        topNProductList.set(j + 1, topNProductList.get(j));
                                    }
                                    topNProductList.set(i, productCountEntry);
                                    bigger = true;
                                    break;
                                }
                            }
                            // 若LRUMap的当前元素比topN结果集中所有元素都小，且topN结果集还没满，则直接放入最后一个位置
                            if (!bigger && topNProductList.size() < n) {
                                topNProductList.add(productCountEntry);
                            }
                        }
                    }

                    // 每次都将自己的热门商品列表，写入自己的taskid对应的zookeeper节点
                    List<Long> topNProductIds = topNProductList
                            .stream()
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toList());
                    String topNProductIdsJSON = JSONArray.toJSONString(topNProductIds);
                    // 更新自己的taskid对应的热门商品列表
                    String thisTaskPath = ProductCountBolt.getTopN4ThisTaskPath(taskid);
                    zkUtils.createNode(thisTaskPath);
                    zkUtils.setNodeData(thisTaskPath, topNProductIdsJSON);
                    log.info("当前task统计的topN结果为:{}", topNProductIdsJSON);
                    // 清除此task的热门商品列表是否已被预热的标记节点
                    zkUtils.deleteNode("/taskid-status-" + taskid);
                    // 休眠1分钟
                    Utils.sleep(60000);
                } catch (Exception e) {
                    log.error("【热门商品列表统计】失败，taskid=" + taskid, e);
                }
            }
        }
    }


    /**
     * 每 5s 进行一次热点发现(达到平均访问次数值的10倍)，用于Nginx缓存以及流量分发策略自动降级，详细如下：
     * 1.请求流量分发的nginx触发流量分发策略降级的接口（在nginx缓存该productId的"热点"标记，在处理客户端请求时若热点标记为true则降级为随机负载均衡，否则走hash策略）
     * 2.请求商品服务查询该热点商品的数据，并将热点的数据反向推送到所有应用层nginx中缓存起来
     */
    private class HotProductFindThread implements Runnable {
        @Override
        public void run() {
            log.info("热点发现线程正在运行...");
            List<Map.Entry<Long, Long>> productCountList = new ArrayList<>(); // 当前LRUMap的访问次数排序后的结果集
            List<Long> hotProductIdList = new ArrayList<>();                  // 热点商品结果集
            List<Long> lastTimeHotProductIds = new ArrayList<>();             //暂存上一次发现的热点商品

            while (true) {
                // 1、将LRUMap中的数据按照访问次数，进行全局的排序
                // 2、计算95%的商品的访问次数的平均值
                // 3、遍历排序后的商品访问次数，从最大的开始
                // 4、如果某个商品比如它的访问量是平均值的10倍，就认为是热点
                try {
                    productCountList.clear();
                    hotProductIdList.clear();

                    if (productCountMap.size() == 0) {
                        Utils.sleep(100);
                        continue;
                    }

                    // 1、先做全局的排序，排序算法与ProductCountThread一样
                    for (Map.Entry<Long, Long> productCountEntry : productCountMap.entrySet()) {
                        if (productCountList.size() == 0) {
                            productCountList.add(productCountEntry);
                        } else {
                            boolean bigger = false;

                            for (int i = 0; i < productCountList.size(); i++) {
                                if (productCountEntry.getValue() > productCountList.get(i).getValue()) {
                                    // 最后一个元素的下标
                                    int lastIndex = productCountList.size() < productCountMap.size() ? productCountList.size() - 1 : productCountMap.size() - 2;
                                    for (int j = lastIndex; j >= i; j--) {
                                        if (j + 1 == productCountList.size()) {
                                            productCountList.add(null);
                                        }
                                        productCountList.set(j + 1, productCountList.get(j));
                                    }
                                    productCountList.set(i, productCountEntry);
                                    bigger = true;
                                    break;
                                }
                            }
                            if (!bigger) {
                                if (productCountList.size() < productCountMap.size()) {
                                    productCountList.add(productCountEntry);
                                }
                            }
                        }
                    }
                    // 2、取出商品访问次数LRUMap中的后95%条数据，计算出商品访问次数的平均值
                    Long totalCount = 0L;  //后95%条数据的总访问量
                    int calculateCount = Math.max(1, (int) Math.floor(productCountList.size() * 0.95));  //第95%条数据下标(保证>0)
                    for (int i = productCountList.size() - 1; i >= productCountList.size() - calculateCount; i--) {
                        totalCount += productCountList.get(i).getValue();
                    }
                    long avgCount = totalCount / calculateCount;
                    log.info("计算出LRUMap中后95%的商品的访问次数平均值avgCount={}，totalCount={}，calculateCount={}", avgCount, totalCount, calculateCount);

                    // 3、从第一个元素开始遍历，超过平均访问次数值的10倍则视为热点
                    for (Map.Entry<Long, Long> productCountEntry : productCountList) {
                        if (productCountEntry.getValue() <= 10 * avgCount) {
                            continue;
                        }
                        log.info("发现一个热点，productCountEntry={}", productCountEntry);
                        Long productId = productCountEntry.getKey();
                        hotProductIdList.add(productId);
                        // 若上次已经有发现此热点则跳过
                        if (lastTimeHotProductIds.contains(productId)) {
                            log.info("上次已经发现过此热点，忽略，productCountEntry={}", productCountEntry);
                            continue;
                        }
                        // 请求流量分发的nginx触发流量分发策略降级的接口（在nginx缓存该productId的"热点"标记，在处理客户端请求时若热点标记为true则降级为随机负载均衡，否则走hash策略）
                        String distributeNginxURL = "http://192.168.11.102/hot?productId=" + productId;
                        HttpClientUtils.sendGetRequest(distributeNginxURL);
                        // 请求商品服务查询该热点商品的数据，并将热点的数据反向推送到所有应用层nginx中缓存起来
                        String cacheServiceURL = "http://192.168.101.10:8080/getProductInfo?productId=" + productId;
                        String response = HttpClientUtils.sendGetRequest(cacheServiceURL);
                        List<NameValuePair> params = new ArrayList<>();
                        params.add(new BasicNameValuePair("productInfo", response));
                        String productInfo = URLEncodedUtils.format(params, StandardCharsets.UTF_8);
                        String[] appNginxURLs = new String[]{
                                "http://192.168.11.103/hot?productId=" + productId + "&" + productInfo,
                                "http://192.168.11.104/hot?productId=" + productId + "&" + productInfo
                        };
                        for (String appNginxURL : appNginxURLs) {
                            HttpClientUtils.sendGetRequest(appNginxURL);
                        }
                        log.info("已通知分发层nginx流量分发策略降级，已把热点数据发送给应用层nginx缓存，productCountEntry={}", productCountEntry);
                    }
                    // 4、处理消失的热点数据
                    // 若第一次发现热点则直接放入上次发现的热点列表
                    if (CollectionUtils.isEmpty(lastTimeHotProductIds) &&
                            CollectionUtils.isNotEmpty(hotProductIdList)) {
                        lastTimeHotProductIds.addAll(hotProductIdList);
                        log.info("保存上次热点数据，lastTimeHotProductIdList={}", lastTimeHotProductIds);
                    } else {   // 否则与上一次的热点进行比对，请求取消已消失的热点的nginx缓存
                        for (Long productId : lastTimeHotProductIds) {
                            if (!hotProductIdList.contains(productId)) {
                                log.info("发现一个热点消失了，productId={}", productId);
                                // 发送一个http请求给到流量分发的nginx中，取消热点缓存的标识
                                String url = "http://192.168.11.102/cancel_hot?productId=" + productId;
                                HttpClientUtils.sendGetRequest(url);
                            }
                        }
                        // 清空上次发现的热点列表，若本次有发现热点商品还需要更新为上次发现的热点列表
                        lastTimeHotProductIds.clear();
                        if (CollectionUtils.isNotEmpty(hotProductIdList)) {
                            lastTimeHotProductIds.addAll(hotProductIdList);
                            log.info("保存上次热点数据，lastTimeHotProductIdList={}", lastTimeHotProductIds);
                        }
                    }

                    Utils.sleep(5000);
                } catch (Exception e) {
                    log.error("【热点发现】任务执行失败", e);
                }
            }
        }
    }


    /**
     * 存放该 storm task 自己的热门商品列表的 zk node path
     *
     * @param taskid storm任务id
     * @return taskid对应的zookeeper节点
     */
    public static String getTopN4ThisTaskPath(int taskid) {
        return "/task-hot-product-list-" + taskid;
    }

    /**
     * 获取锁定目录
     */
    public static String getLockPath() {
        return "/taskid-list-lock";
    }

}
