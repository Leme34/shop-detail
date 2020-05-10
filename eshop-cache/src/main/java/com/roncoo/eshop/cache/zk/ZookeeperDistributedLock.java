package com.roncoo.eshop.cache.zk;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * zookeeper分布式锁
 * <p>
 * Created by lsd
 * 2020-05-09 23:45
 */
@Slf4j
public class ZookeeperDistributedLock {

    private final static String ZOOKEEPER_SERVERS = "192.168.11.102:2181";   //以逗号分隔的zookeeper地址

    private ZooKeeper zooKeeper;
    private final static CountDownLatch connectedSemaphore = new CountDownLatch(1);

    /**
     * 封装单例的静态内部类
     */
    static class Singleton {
        private static ZookeeperDistributedLock zookeeperDistributedLock;

        static {
            zookeeperDistributedLock = new ZookeeperDistributedLock();
        }

        public static ZookeeperDistributedLock getInstance() {
            return Singleton.zookeeperDistributedLock;
        }
    }

    /**
     * 获取单例
     */
    public static ZookeeperDistributedLock getInstance() {
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
    public ZookeeperDistributedLock() {
        try {
            // 异步创建zk客户端，会返回一个状态CONNECTING（连接中）
            this.zooKeeper = new ZooKeeper(
                    ZOOKEEPER_SERVERS,
                    60000,
                    // 监听初始化完成事件，连接建立完成则打开门闩
                    (WatchedEvent event) -> {
                        log.debug("zk session watcher接收到事件: {}", event.getState());
                        if (Watcher.Event.KeeperState.SyncConnected == event.getState()) {
                            connectedSemaphore.countDown();
                        }
                    });
            log.debug("正在创建zooKeeper会话，状态：{}", zooKeeper.getState());
            // 等待初始化完成
            connectedSemaphore.await();
            log.debug("zooKeeper会话连接已建立");
        } catch (Exception e) {
            log.error("创建zooKeeper会话连接失败", e);
        }
    }

    /**
     * 自旋获取分布式锁，获取失败会每隔200ms进行重试，直到成功获得锁
     *
     * @param productId 商品id
     */
    public void acquireDistributedLock(Long productId) {
        String path = getLockPath(productId);
        // 创建临时节点，默认acl权限
        try {
            zooKeeper.create(path, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            log.debug("成功获取分布式锁，productId={}", productId);
        } catch (Exception e) {  //每隔200ms进行重试，直到成功获得锁
            log.debug("获取分布式锁失败，productId={}，原因:{}", productId, e.getMessage());
            int count = 0;
            while (true) {
                try {
                    TimeUnit.MILLISECONDS.sleep(200);
                    zooKeeper.create(path, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                } catch (Exception ex) {
                    log.debug("获取分布式锁失败，productId={}，原因:{}", productId, e.getMessage());
                    count++;
                    continue;
                }
                // 没抛异常则成功
                log.debug("获取分布式锁成功，productId={}，已重试次数:{}", productId, count);
                break;
            }
        }
    }


    /**
     * 释放分布式锁
     *
     * @param productId 商品id
     */
    public boolean releaseDistributedLock(Long productId) {
        String path = getLockPath(productId);
        try {
            zooKeeper.delete(path, -1);
            log.debug("分布式锁释放成功，productId={}", productId);
            return true;
        } catch (Exception e) {
            if (e instanceof KeeperException.NoNodeException) {
                log.error("分布式锁节点不存在，productId=" + productId, e);
                return true;
            }
            log.error("分布式锁释放失败", e);
            return false;
        }
    }

    private String getLockPath(Long productId) {
        return "/product-lock-" + productId;
    }

}
