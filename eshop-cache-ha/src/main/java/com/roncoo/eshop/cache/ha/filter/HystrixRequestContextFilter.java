package com.roncoo.eshop.cache.ha.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import lombok.extern.slf4j.Slf4j;

/**
 * hystrix请求上下文过滤器
 */
@Slf4j
public class HystrixRequestContextFilter implements Filter {

    public void init(FilterConfig config) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        // 在每个请求的开始处调用此函数，以初始化请求上下文
        // 请求上下文相同的请求会直接由 Hystrix 请求缓存中返回
        HystrixRequestContext context = HystrixRequestContext.initializeContext();
        try {
            chain.doFilter(request, response);
        } catch (Exception e) {
            log.error("hystrix请求上下文过滤器出错", e);
        } finally {
            context.shutdown();
        }
    }

    public void destroy() {
    }

}
