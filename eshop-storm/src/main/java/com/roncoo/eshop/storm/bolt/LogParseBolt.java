package com.roncoo.eshop.storm.bolt;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import com.alibaba.fastjson.JSONObject;

/**
 * Nginx日志解析的bolt
 */
@Slf4j
public class LogParseBolt extends BaseRichBolt {

    private static final long serialVersionUID = -8017609899644290359L;

    private OutputCollector collector;

    /**
     * 对于bolt来说，第一个方法，就是prepare方法
     * <p>
     * OutputCollector，这个也是Bolt的这个tuple的发射器
     */
    @SuppressWarnings("rawtypes")
    public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
    }


    /**
     * execute方法
     * <p>
     * 就是说，每次接收到一条数据后，就会交给这个executor方法来执行
     */
    public void execute(Tuple tuple) {
        // 从日志中提取出productId，并发射出去
        String message = tuple.getStringByField("message");
        log.info("接收到一条消息：{}", message);

        JSONObject messageJSON = JSONObject.parseObject(message);
        JSONObject uriArgsJSON = messageJSON.getJSONObject("uri_args");
        Long productId = uriArgsJSON.getLong("productId");

        if (productId != null) {
            collector.emit(new Values(productId));
            log.info("发射一个商品id={}", productId);
        }
    }

    /**
     * 定义发射出去的tuple，每个field的名称
     */
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("productId"));
    }

}
