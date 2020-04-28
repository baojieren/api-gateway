package com.gmsj.route;

import com.gmsj.base.YmlConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * @author baojieren
 * @date 2020/4/14 17:57
 */
@Slf4j
@Configuration
public class RouteConfig {

    @Resource
    private YmlConfig ymlConfig;

    // @Bean
    // public RouteLocator customerRouteLocator(RouteLocatorBuilder builder) {
    //     return builder.routes()
    //             .route("authRouter", r -> r.path("/auth/**").uri(ymlConfig.getAuthUrl()).order(0))
    //             .route("miniRouter", r -> r.path("/mini/**").uri(ymlConfig.getMiniUrl()).order(0))
    //             .route("mngRouter", r -> r.path("/mng/**").uri(ymlConfig.getMngUrl()).order(0))
    //             .build();
    //
    // }

}
