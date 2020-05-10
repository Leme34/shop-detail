package com.roncoo.eshop.storm;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 单词计数拓扑
 * <p>
 * 我认识很多java工程师，都是会一些大数据的技术的，不会太精通，没有那么多的时间去研究
 * storm的课程，我就只是讲到，最基本的开发，就够了，java开发广告计费系统，大量的流量的引入和接入，就是用storm做得
 * 用storm，主要是用它的成熟的稳定的易于扩容的分布式系统的特性
 * java工程师，来说，做一些简单的storm开发，掌握到这个程度差不多就够了
 *
 * @author Administrator
 */
public class WordCountTopology {

    /**
     * spout
     * <p>
     * spout，继承一个基类，实现接口，这个里面主要是负责从数据源获取数据
     * <p>
     * 我们这里作为一个简化，就不从外部的数据源去获取数据了，只是自己内部不断发射一些句子
     */
    public static class RandomSentenceSpout extends BaseRichSpout {

        private static final long serialVersionUID = 3699352201538354417L;

        private static final Logger LOGGER = LoggerFactory.getLogger(RandomSentenceSpout.class);

        private SpoutOutputCollector collector;
        private Random random;

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
            // 在open方法初始化的时候，会传入进来一个东西，叫做SpoutOutputCollector
            // 这个SpoutOutputCollector就是用来发射数据出去的
            this.collector = collector;
            // 构造一个随机数生产对象
            this.random = new Random();
        }

        /**
         * nextTuple方法
         * <p>
         * 这个spout类，之前说过，最终会运行在task中，某个worker进程的某个executor线程内部的某个task中
         * 那个task会负责去不断的无限循环调用nextTuple()方法
         * 只要的话呢，无限循环调用，可以不断发射最新的数据出去，形成一个数据流
         */
        public void nextTuple() {
            Utils.sleep(100);
            String[] sentences = new String[]{"the cow jumped over the moon", "an apple a day keeps the doctor away",
                    "four score and seven years ago", "snow white and the seven dwarfs", "i am at two with nature"};
            // 随机一些句子
            String sentence = sentences[random.nextInt(sentences.length)];
            LOGGER.info("【发射句子】sentence=" + sentence);
            // 这个values，你可以认为就是构建一个tuple
            // tuple是最小的数据单位，无限个tuple组成的流就是一个stream
            // 把此次随机的句子发射出去
            collector.emit(new Values(sentence));
        }

        /**
         * declareOutputFielfs这个方法
         * <p>
         * 很重要，这个方法是定义一个你发射出去的每个tuple中的每个field的名称是什么
         */
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("sentence"));
        }

    }

    /**
     * 写一个bolt，直接继承一个BaseRichBolt基类
     * <p>
     * 实现里面的所有的方法即可，每个bolt代码，同样是发送到worker某个executor的task里面去运行
     *
     * @author Administrator
     */
    public static class SplitSentence extends BaseRichBolt {

        private static final long serialVersionUID = 6604009953652729483L;

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
            String sentence = tuple.getStringByField("sentence");
            String[] words = sentence.split(" ");
            // 把分割后的词全部发送出去
            for (String word : words) {
                collector.emit(new Values(word));
            }
        }

        /**
         * 定义发射出去的tuple，每个field的名称
         */
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("word"));
        }

    }


    /**
     * bolt，用于封装计算逻辑，spout会将数据传送给bolt，各种bolt还可以串联成一个计算链条
     */
    public static class WordCount extends BaseRichBolt {

        private static final long serialVersionUID = 7208077706057284643L;

        private static final Logger LOGGER = LoggerFactory.getLogger(WordCount.class);

        private OutputCollector collector;
        private Map<String, Long> wordCounts = new HashMap<>();

        @SuppressWarnings("rawtypes")
        public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
            this.collector = collector;
        }

        /**
         * @param tuple tuple：就是一条数据，每条数据都会被封装在tuple中，在多个spout和bolt之间传递
         */
        public void execute(Tuple tuple) {
            String word = tuple.getStringByField("word");

            Long count = wordCounts.get(word);
            if (count == null) {
                count = 0L;
            }
            count++;

            wordCounts.put(word, count);

            LOGGER.info("【单词计数】" + word + "出现的次数是" + count);

            collector.emit(new Values(word, count));
        }

        /**
         * 声明此拓扑的所有流的输出模式
         * 一堆spout+bolt，就会组成一个topology(拓扑)
         */
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("word", "count"));
        }

    }

    public static void main(String[] args) {
        // 在main方法中，会去将spout和bolts组合起来，构建成一个拓扑
        TopologyBuilder builder = new TopologyBuilder();

        // 这里的第一个参数的意思，就是给这个spout设置一个名字
        // 第二个参数的意思，就是创建一个spout的对象
        // 第三个参数的意思，就是设置spout的executor有几个
        builder.setSpout("RandomSentence", new RandomSentenceSpout(), 2);
        builder.setBolt("SplitSentence", new SplitSentence(), 5)
                .setNumTasks(10)                            //若不设置则与spout的executor数量一样
                .shuffleGrouping("RandomSentence");            //流分组策略
        // 这个很重要，就是说，相同的单词，从SplitSentence发射出来时，一定会进入到下游的指定的同一个task中
        // 只有这样子，才能准确的统计出每个单词的数量
        // 比如你有个单词，hello，下游task1接收到3个hello，task2接收到2个hello
        // 5个hello，全都进入一个task
        // 因此需要使用 fieldsGrouping 作为流分组策略
        builder.setBolt("WordCount", new WordCount(), 10)
                .setNumTasks(20)
                .fieldsGrouping("SplitSentence", new Fields("word"));   //流分组策略

        // storm配置
        Config config = new Config();
        // 若参数不为空，则说明是在命令行打算提交到storm集群上去
        if (args != null && args.length > 0) {
            config.setNumWorkers(3);
            try {
                StormSubmitter.submitTopology(args[0], config, builder.createTopology());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {    // 否则说明是在IDE里面本地运行
            config.setMaxTaskParallelism(20);
            LocalCluster cluster = new LocalCluster();
            cluster.submitTopology("WordCountTopology", config, builder.createTopology());

            // 打印1分钟就停止
            Utils.sleep(60000);
            cluster.shutdown();
        }
    }

}
