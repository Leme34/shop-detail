package com.roncoo.eshop.storm.bolt;

import java.util.Map;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.trident.util.LRUMap;
import org.apache.storm.tuple.Tuple;

/**
 * 商品访问次数统计bolt
 * @author Administrator
 *
 */
public class ProductCountBolt extends BaseRichBolt {

	private static final long serialVersionUID = -8761807561458126413L;

	private LRUMap<Long, Long> productCountMap = new LRUMap<>(1000);

	@SuppressWarnings("rawtypes")
	public void prepare(Map conf, TopologyContext context, OutputCollector collector) {

	}

	public void execute(Tuple tuple) {
		Long productId = tuple.getLongByField("productId");

		Long count = productCountMap.get(productId);
		if(count == null) {
			count = 0L;
		}
		count++;

		productCountMap.put(productId, count);
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {

	}

}
