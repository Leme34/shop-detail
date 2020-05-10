package com.roncoo.eshop.storm;

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
 * @author Administrator
 *
 */
public class HotProductTopology {

	public static void main(String[] args) {
		TopologyBuilder builder = new TopologyBuilder();
	
		builder.setSpout("AccessLogKafkaSpout", new AccessLogKafkaSpout(), 1);
		builder.setBolt("LogParseBolt", new LogParseBolt(), 5)
				.setNumTasks(5)
				.shuffleGrouping("AccessLogKafkaSpout");  
		builder.setBolt("ProductCountBolt", new ProductCountBolt(), 5)
				.setNumTasks(10)
				.fieldsGrouping("LogParseBolt", new Fields("productId"));  
		
		Config config = new Config();
		
		if(args != null && args.length > 1) {
			config.setNumWorkers(3);  
			try {
				StormSubmitter.submitTopology(args[0], config, builder.createTopology());
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			LocalCluster cluster = new LocalCluster();
			cluster.submitTopology("HotProductTopology", config, builder.createTopology());  
			Utils.sleep(30000); 
			cluster.shutdown();
		}
	}
	
}
