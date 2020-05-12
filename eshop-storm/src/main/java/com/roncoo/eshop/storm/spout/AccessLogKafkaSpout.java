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

import lombok.extern.slf4j.Slf4j;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;

/**
 * 消费kafka数据的spout
 */
@Slf4j
public class AccessLogKafkaSpout extends BaseRichSpout {

    private static final long serialVersionUID = 8698470299234327074L;

    // 用于存放从Kafka拉取到的消息
    private ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(1000);

    private SpoutOutputCollector collector;

    /**
     * open方法
     * <p>
     * open方法，是对spout进行初始化的
     * <p>
     * 比如说，创建一个线程池，或者创建一个数据库连接池，或者构造一个httpclient
     */
    @SuppressWarnings("rawtypes")
    public void open(Map conf, TopologyContext context,
                     SpoutOutputCollector collector) {
        this.collector = collector;
        this.startKafkaConsumer();
    }

    @SuppressWarnings("rawtypes")
    private void startKafkaConsumer() {
        Properties props = new Properties();
        props.put("zookeeper.connect", "192.168.11.102:2181");
        props.put("group.id", "eshop-storm");
        props.put("zookeeper.session.timeout.ms", "40000");
        props.put("zookeeper.sync.time.ms", "200");
        props.put("auto.commit.interval.ms", "1000");
        ConsumerConfig consumerConfig = new ConsumerConfig(props);

        ConsumerConnector consumerConnector = Consumer.
                createJavaConsumerConnector(consumerConfig);
        String topic = "access-log";

        Map<String, Integer> topicCountMap = new HashMap<>();
        topicCountMap.put(topic, 1);

        Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap =
                consumerConnector.createMessageStreams(topicCountMap);
        List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(topic);

        for (KafkaStream stream : streams) {
            new Thread(new KafkaMessageProcessor(stream)).start();
        }
    }

    /**
     * Kafka消费线程
     */
    private class KafkaMessageProcessor implements Runnable {

        @SuppressWarnings("rawtypes")
        private KafkaStream kafkaStream;

        @SuppressWarnings("rawtypes")
        public KafkaMessageProcessor(KafkaStream kafkaStream) {
            this.kafkaStream = kafkaStream;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            ConsumerIterator<byte[], byte[]> it = kafkaStream.iterator();
            while (it.hasNext()) {
                String message = new String(it.next().message());
                log.info("接收到kafka消息：{}", message);
                try {
                    queue.put(message);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    /**
     * nextTuple方法
     * <p>
     * 这个spout类，之前说过，最终会运行在task中，某个worker进程的某个executor线程内部的某个task中
     * 那个task会负责去不断的无限循环调用nextTuple()方法
     * 只要的话呢，无限循环调用，可以不断发射最新的数据出去，形成一个数据流
     */
    public void nextTuple() {
        // 把接收到的kafka消息发射出去，若没有接收到kafka消息则休眠100毫秒
        if (queue.size() > 0) {
            try {
                String message = queue.take();
                collector.emit(new Values(message));
                log.info("发射一条kafka消息：{}", message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Utils.sleep(100);
        }
    }


    /**
     * declareOutputFields这个方法很重要，这个方法是定义一个你发射出去的每个tuple中的每个field的名称是什么
     */
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("message"));
    }

}
