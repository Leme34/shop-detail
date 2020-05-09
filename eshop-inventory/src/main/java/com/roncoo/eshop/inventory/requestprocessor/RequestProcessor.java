package com.roncoo.eshop.inventory.requestprocessor;

import com.roncoo.eshop.inventory.request.Request;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;

/**
 * 执行请求的Callable任务
 */
@Slf4j
public class RequestProcessor implements Callable<Boolean> {

    /**
     * 线程自己监控的内存队列
     */
    private ArrayBlockingQueue<Request> queue;

    public RequestProcessor(ArrayBlockingQueue<Request> queue) {
        this.queue = queue;
    }

    @Override
    public Boolean call() {
        try {
            while (true) {
                // 消费
                Request request = queue.take();
                log.debug("开始处理队列中的请求，商品id={}", request.getProductId());
                request.process();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

}
