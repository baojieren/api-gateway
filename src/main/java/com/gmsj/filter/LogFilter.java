package com.gmsj.filter;

import com.gmsj.common.constant.HttpHeaderConstant;
import com.gmsj.common.util.JwtUtil;
import com.gmsj.util.IpUtil;
import com.gmsj.util.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * 统一日志打印
 *
 * @author renbaojie
 * @date 2020/4/12 16:39
 */
@Slf4j
@Component
public class LogFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getAttributes().put("startTime", System.currentTimeMillis());
        ServerHttpRequest request = exchange.getRequest();
        String uri = request.getURI().getPath();
        String ip = IpUtil.getRemoteIP(request);
        String authorization = request.getHeaders().getFirst(HttpHeaderConstant.HEADER_AUTH);

        // 解析token拿用户id
        String userId = null;
        if (!StringUtils.isEmpty(authorization) && authorization.startsWith(HttpHeaderConstant.HEADER_SCHEME)) {
            try {
                String token = authorization.split(" ")[1];
                userId = JwtUtil.getUserId(token);
            } catch (Exception e) {
                log.error("解析token拿userId出错 {}", e.toString());
            }
        }

        String traceId = TraceIdUtil.createTraceId();
        log.info("请求--->: {},\ttraceId: {},\tuserId: {},\tIP: {}"
                , uri
                , traceId
                , userId
                , ip);

        ServerHttpRequest addHeaders = request.mutate().headers(httpHeaders -> {
            httpHeaders.set(HttpHeaderConstant.HEADER_IP, ip);
            httpHeaders.set(HttpHeaderConstant.HEADER_TRACEID, traceId);
        }).build();
        return chain.filter(exchange.mutate().request(addHeaders).build()).then(
                Mono.fromRunnable(() -> {
                    Object startTime = exchange.getAttribute("startTime");
                    if (!ObjectUtils.isEmpty(startTime)) {
                        log.info("响应<---: {},\ttraceId: {},\t耗时: {}ms"
                                , uri
                                , traceId
                                , System.currentTimeMillis() - (long) startTime);
                    }
                })
        );
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
