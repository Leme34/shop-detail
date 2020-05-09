package com.roncoo.eshop.inventory.request;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单例的请求内存队列
 */
public class RequestQueue {

	/**
	 * 内存队列
	 */
	private List<ArrayBlockingQueue<Request>> queues = new ArrayList<>();

	/**
	 * 商品请求是否已存在队列的标记Map
	 * null:队列中【不存在】该商品库存的任何【读/写】请求
	 * 0:队列中【存在】该商品库存的【写】请求
	 * 1:队列中【存在】该商品库存的【读】请求
	 */
	@Getter
	private Map<Integer,Integer> productId2FlagMap = new ConcurrentHashMap<>();

	/**
	 * 单例有很多种方式去实现：我采取绝对线程安全的一种方式
	 * 静态内部类的方式，去初始化单例
	 */
	private static class Singleton {

		private static RequestQueue instance;

		static {
			instance = new RequestQueue();
		}

		public static RequestQueue getInstance() {
			return instance;
		}

	}

	/**
	 * jvm的机制去保证多线程并发安全
	 * 内部类的初始化，一定只会发生一次，不管多少个线程并发去初始化
	 */
	public static RequestQueue getInstance() {
		return Singleton.getInstance();
	}

	/**
	 * 往内存队列中添加一个阻塞队列
	 * @param queue
	 */
	public void addQueue(ArrayBlockingQueue<Request> queue) {
		this.queues.add(queue);
	}

	/**
	 * 获取内存队列的大小
	 * @return
	 */
	public int queueSize() {
		return queues.size();
	}

	/**
	 * 获取内存队列中的目标阻塞队列
	 * @param index
	 * @return
	 */
	public ArrayBlockingQueue<Request> getQueue(int index) {
		return queues.get(index);
	}
}
