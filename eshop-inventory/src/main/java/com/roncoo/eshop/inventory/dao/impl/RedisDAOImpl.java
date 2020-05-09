package com.roncoo.eshop.inventory.dao.impl;

import javax.annotation.Resource;

import org.springframework.stereotype.Repository;

import redis.clients.jedis.JedisCluster;

import com.roncoo.eshop.inventory.dao.RedisDAO;

@Repository("redisDAO")
public class RedisDAOImpl implements RedisDAO {

	@Resource
	private JedisCluster jedisCluster;
	
	@Override
	public void set(String key, String value) {
		jedisCluster.set(key, value);
	}

	@Override
	public String get(String key) {
		return jedisCluster.get(key);
	}

	@Override
	public void delete(String key) {
		jedisCluster.del(key);
	}

}
