package com.gmsj.filter;

import com.gmsj.base.YmlConfig;
import com.gmsj.common.constant.HttpHeaderConstant;
import com.gmsj.common.util.JwtUtil;
import com.gmsj.model.AuthPathBo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局授权拦截器
 *
 * @author baojieren
 * @date 2020/4/15 17:10
 */
@Slf4j
@Component
public class PathAuthFilter implements GlobalFilter, Ordered {
    @Resource
    YmlConfig ymlConfig;
    @Resource
    StringRedisTemplate stringRedisTemplate;

    /**
     * 检验规则:
     * 1.先判断访问路径是否需要鉴权, 不需要则直接放过
     * 2.判断是否携带token,没有直接拒绝
     * 3.判断token是否合法
     * 4.判断redis是否存在用户信息(登录时会把用户基本信息存redis)
     * 5.判断是否是admin,是就直接放过
     * 6.判断权限
     * <p>
     * 提升:
     * nacos配置的匹配规则需要符合ant规范
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 判断路径是否属于不授权规则
        AntPathMatcher matcher = new AntPathMatcher();
        for (String anonReg : ymlConfig.getAnonPathList()) {
            if (matcher.match(anonReg, path)) {
                return chain.filter(exchange);
            }
        }

        String token = JwtUtil.getTokenFromAuthorization(request.getHeaders().getFirst(HttpHeaderConstant.HEADER_AUTH));
        if (StringUtils.isEmpty(token) || JwtUtil.isTokenExpired(token)) {
            log.warn("没有鉴权信息或已过期");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 判断token是否存在
        String userId = JwtUtil.getUserId(token);
        String userToken = stringRedisTemplate.opsForValue().get("token:" + userId);
        if (StringUtils.isEmpty(userToken)) {
            log.warn("非法Token");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 查询用户信息
        String redisUserInfo = stringRedisTemplate.opsForValue().get(HttpHeaderConstant.HEADER_USER_INFO + ":" + userId);
        if (StringUtils.isEmpty(redisUserInfo)) {
            log.warn("用户信息已过期,请重新登录");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String userInfoEncode = "";
        try {
            userInfoEncode = URLEncoder.encode(redisUserInfo, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("编码redisUserInfo失败");
            e.printStackTrace();
        }
        String userInfo = userInfoEncode;
        String[] userRoleArr = JwtUtil.getRoles(token);
        if (Arrays.asList(userRoleArr).contains("admin")) {
            ServerHttpRequest addHeaders = request.mutate().headers(httpHeaders -> {
                httpHeaders.set(HttpHeaderConstant.HEADER_USER_INFO, userInfo);
            }).build();
            return chain.filter(exchange.mutate().request(addHeaders).build());
        }

        Map<String, List<AuthPathBo>> pathMap = ymlConfig.getAuthPathList().stream().collect(Collectors.groupingBy(AuthPathBo::getPath));
        List<String> pathList = ymlConfig.getAuthPathList().stream().map(AuthPathBo::getPath).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(pathList)) {
            for (String p : pathList) {
                if (matcher.match(p, path)) {
                    // 匹配到符合的url规则,开始检验权限
                    AuthPathBo authPathBo = pathMap.get(p).get(0);
                    if (!StringUtils.isEmpty(authPathBo.getRoles())) {
                        String[] pathRoleArr = authPathBo.getRoles().split(",");
                        for (String pathRole : pathRoleArr) {
                            for (String userRole : userRoleArr) {
                                if (pathRole.equals(userRole)) {
                                    ServerHttpRequest addHeaders = request.mutate().headers(httpHeaders -> {
                                        httpHeaders.set(HttpHeaderConstant.HEADER_USER_INFO, userInfo);
                                    }).build();
                                    return chain.filter(exchange.mutate().request(addHeaders).build());
                                }
                            }
                        }
                        log.warn("权限不足,拒绝访问");
                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                        return exchange.getResponse().setComplete();
                    }
                    ServerHttpRequest addHeaders = request.mutate().headers(httpHeaders -> {
                        httpHeaders.set(HttpHeaderConstant.HEADER_USER_INFO, userInfo);
                    }).build();
                    return chain.filter(exchange.mutate().request(addHeaders).build());
                }
            }
        }
        ServerHttpRequest addHeaders = request.mutate().headers(httpHeaders -> {
            httpHeaders.set(HttpHeaderConstant.HEADER_USER_INFO, userInfo);
        }).build();
        return chain.filter(exchange.mutate().request(addHeaders).build());
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
