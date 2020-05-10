package com.roncoo.eshop.storm.spout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;

import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;

/**
 * kafka消费数据的spout
 */
public class AccessLogKafkaSpout extends BaseRichSpout {

	private static final long serialVersionUID = 8698470299234327074L;

	private ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<String>(1000);
	
	private SpoutOutputCollector collector;
	
	@SuppressWarnings("rawtypes")
	public void open(Map conf, TopologyContext context,
			SpoutOutputCollector collector) {
		this.collector = collector;
		startKafkaConsumer();
	}
	
	@SuppressWarnings("rawtypes")
	private void startKafkaConsumer() {
		Properties props = new Properties();
        props.put("zookeeper.connect", "192.168.31.187:2181,192.168.31.19:2181,192.168.31.227:2181");
        props.put("group.id", "eshop-cache-group");
        props.put("zookeeper.session.timeout.ms", "40000");
        props.put("zookeeper.sync.time.ms", "200");
        props.put("auto.commit.interval.ms", "1000");
        ConsumerConfig consumerConfig = new ConsumerConfig(props);
		
		ConsumerConnector consumerConnector = Consumer.
				createJavaConsumerConnector(consumerConfig);
		String topic = "access-log";
		
		Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
        topicCountMap.put(topic, 1);
        
        Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = 
        		consumerConnector.createMessageStreams(topicCountMap);
        List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(topic);
        
        for (KafkaStream stream : streams) {
            new Thread(new KafkaMessageProcessor(stream)).start();
        }
	}
	
	private class KafkaMessageProcessor implements Runnable {

		@SuppressWarnings("rawtypes")
		private KafkaStream kafkaStream;
		
		@SuppressWarnings("rawtypes")
		public KafkaMessageProcessor(KafkaStream kafkaStream) {
			this.kafkaStream = kafkaStream;
		}
		
		@SuppressWarnings("unchecked")
		public void run() {
			ConsumerIterator<byte[], byte[]> it = kafkaStream.iterator();
	        while (it.hasNext()) {
	        	String message = new String(it.next().message());
	        	try {
					queue.put(message);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} 
	        }
		}
		
	}
	
	public void nextTuple() {
		if(queue.size() > 0) {
			try {
				String message = queue.take();
				collector.emit(new Values(message));  
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			Utils.sleep(100);  
		}
	}
	 
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("message"));  
	}
	
}
