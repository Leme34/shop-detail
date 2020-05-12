package com.roncoo.eshop.storm.bolt;

import java.util.*;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.roncoo.eshop.storm.zk.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
     * 每分钟计算1次topN结果集
     */
    private class ProductCountThread implements Runnable {
        @Override
        public void run() {
            // topN结果集
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
                                    int lastIndex = topNProductList.size() < n ? topNProductList.size() - 1 : n - 2;
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
                    String thisTaskPath = ProductCountBolt.getTopN4ThisTaskPath(taskid);
                    zkUtils.createNode(thisTaskPath);
                    zkUtils.setNodeData(thisTaskPath, topNProductIdsJSON);
                    log.info("当前task统计的topN结果为:{}", topNProductIdsJSON);

                    // 休眠1分钟
                    Utils.sleep(60000);
                } catch (Exception e) {
                    log.error("热门商品列表统计失败，taskid=" + taskid, e);
                }
            }
        }
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
