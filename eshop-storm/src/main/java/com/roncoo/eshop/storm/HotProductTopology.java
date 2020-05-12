package com.roncoo.eshop.storm;

import com.roncoo.eshop.storm.zk.ZookeeperUtils;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;
import org.apache.storm.utils.Utils;

import com.roncoo.eshop.storm.bolt.LogParseBolt;
import com.roncoo.eshop.storm.bolt.ProductCountBolt;
import com.roncoo.eshop.storm.spout.AccessLogKafkaSpout;

/**
 * 热数据统计拓扑
 * <p>
 * 提交到集群中运行的命令：
 * docker-compose exec nimbus storm jar /app.jar com.roncoo.eshop.storm.HotProductTopology HotProductTopology
 */
public class HotProductTopology {

    public static void main(String[] args) {
        // 将spout和bolts组合起来，构建成一个拓扑
        TopologyBuilder builder = new TopologyBuilder();

        builder.setSpout("AccessLogKafkaSpout", new AccessLogKafkaSpout(), 1);
        builder.setBolt("LogParseBolt", new LogParseBolt(), 2)
                .setNumTasks(2)
                .shuffleGrouping("AccessLogKafkaSpout");
        builder.setBolt("ProductCountBolt", new ProductCountBolt(), 2)
                .setNumTasks(2)
                .fieldsGrouping("LogParseBolt", new Fields("productId"));

        // 先清除旧数据
        ZookeeperUtils.getInstance().releaseDistributedLock("/taskid-list");

        Config config = new Config();
        // 若参数不为空，则说明是在命令行打算提交到storm集群上去
        if (args != null && args.length > 0) {
            config.setNumWorkers(3);
            try {
                StormSubmitter.submitTopology(args[0], config, builder.createTopology());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {     // 否则说明是在IDE里面本地运行
            LocalCluster cluster = new LocalCluster();
            cluster.submitTopology("HotProductTopology", config, builder.createTopology());
            Utils.sleep(30000);
            cluster.shutdown();
        }
    }

}
