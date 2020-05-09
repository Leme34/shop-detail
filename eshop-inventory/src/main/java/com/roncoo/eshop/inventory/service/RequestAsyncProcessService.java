package com.roncoo.eshop.inventory.service;

import com.roncoo.eshop.inventory.request.Request;

/**
 * 请求异步执行的service
 *
 */
public interface RequestAsyncProcessService {

	/**
	 * 请求路由，将请求放入对应的队列中，提供去重保证
	 *
	 * @param request 待处理请求
	 */
	void process(Request request);

}
