package com.roncoo.eshop.storm.zk;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * zookeeper分布式锁
 * <p>
 * Created by lsd
 * 2020-05-09 23:45
 */
@Slf4j
public class ZookeeperUtils {

    private final static String ZOOKEEPER_SERVERS = "192.168.11.102:2181";   //以逗号分隔的zookeeper地址

    private ZooKeeper zooKeeper;
    private final static CountDownLatch connectedSemaphore = new CountDownLatch(1);

    /**
     * 封装单例的静态内部类
     */
    static class Singleton {
        private static ZookeeperUtils zookeeperUtils;

        static {
            zookeeperUtils = new ZookeeperUtils();
        }

        public static ZookeeperUtils getInstance() {
            return Singleton.zookeeperUtils;
        }
    }

    /**
     * 获取单例
     */
    public static ZookeeperUtils getInstance() {
        return Singleton.getInstance();
    }

    /**
     * 初始化单例的便捷方法
     */
    public static void init() {
        getInstance();
    }


    /**
     * 会话连接初始化方法
     */
    public ZookeeperUtils() {
        try {
            // 异步创建zk客户端，会返回一个状态CONNECTING（连接中）
            this.zooKeeper = new ZooKeeper(
                    ZOOKEEPER_SERVERS,
                    60000,
                    // 监听初始化完成事件，连接建立完成则打开门闩
                    (WatchedEvent event) -> {
                        log.info("zk session watcher接收到事件: {}", event.getState());
                        if (Watcher.Event.KeeperState.SyncConnected == event.getState()) {
                            connectedSemaphore.countDown();
                        }
                    });
            log.info("正在创建zooKeeper会话，状态：{}", zooKeeper.getState());
            // 等待初始化完成
            connectedSemaphore.await();
            log.info("zooKeeper会话连接已建立");
        } catch (Exception e) {
            log.error("创建zooKeeper会话连接失败", e);
        }
    }

    /**
     * 自旋获取分布式锁，获取失败会每隔200ms进行重试，直到成功获得锁
     */
    public void acquireDistributedLock(String lockPath) {
        // 创建临时节点，默认acl权限
        try {
            zooKeeper.create(lockPath, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            log.info("成功获取分布式锁，lockPath={}", lockPath);
        } catch (Exception e) {  //每隔200ms进行重试，直到成功获得锁
            log.info("获取分布式锁失败，lockPath={}，原因:{}", lockPath, e.getMessage());
            int count = 0;
            while (true) {
                try {
                    TimeUnit.MILLISECONDS.sleep(200);
                    zooKeeper.create(lockPath, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                } catch (Exception ex) {
                    log.info("获取分布式锁失败，lockPath={}，原因:{}", lockPath, e.getMessage());
                    count++;
                    continue;
                }
                // 没抛异常则成功
                log.info("获取分布式锁成功，lockPath={}，已重试次数:{}", lockPath, count);
                break;
            }
        }
    }

    /**
     * 尝试获取分布式锁
     *
     * @return 获取成功：true，获取失败：false
     */
    public boolean tryAcquireDistributedLock(String lockPath) {
        try {
            zooKeeper.create(lockPath, "".getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            log.info("成功获取分布式锁，lockPath={}", lockPath);
            return true;
        } catch (Exception e) {
            log.info("获取分布式锁失败，lockPath={}，原因:{}", lockPath, e.getMessage());
        }
        return false;
    }


    /**
     * 释放分布式锁
     */
    public boolean releaseDistributedLock(String lockPath) {
        try {
            zooKeeper.delete(lockPath, -1);
            log.info("分布式锁释放成功，lockPath={}", lockPath);
            return true;
        } catch (Exception e) {
            if (e instanceof KeeperException.NodeExistsException) {
                log.info("分布式锁节点不存在，忽略本次释放操作，lockPath={}", lockPath);
                return true;
            }
            log.error("分布式锁释放失败", e);
            return false;
        }
    }

    /**
     * 若节点不存在会抛出 KeeperException.NoNodeException，因此调用该方法前必须先调用 {@link #createNode(String)}
     */
    public String getNodeData(String path) {
        try {
            return new String(zooKeeper.getData(path, false, new Stat()));
        } catch (Exception e) {
            log.error("读取zookeeper节点信息失败", e);
            return "";
        }
    }

    /**
     * 若节点不存在会抛出 KeeperException.NoNodeException，因此调用该方法前必须先调用 {@link #createNode(String)}
     */
    public void setNodeData(String path, String data) {
        try {
            zooKeeper.setData(path, data.getBytes(), -1);
        } catch (Exception e) {
            log.error("设置zookeeper节点信息失败", e);
        }
    }

    /**
     * 创建节点，若节点已存在会忽略创建操作
     * 读/写节点数据前都必须先创建出节点
     */
    public void createNode(String path) {
        try {
            zooKeeper.create(path, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (Exception e) {
            if (e instanceof KeeperException.NodeExistsException) {
                log.info("zookeeper节点已存在，忽略本次创建操作");
                return;
            }
            log.error("创建zookeeper节点信息失败", e);
        }
    }

}
