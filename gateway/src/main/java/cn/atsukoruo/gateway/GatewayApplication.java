package cn.atsukoruo.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public RouteLocator primaryRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("authorization", r -> r.path("/authorization/**")
                       .filters(f -> f.stripPrefix(1))
                        .uri("lb://authorization-service"))
                .route("inventory", r -> r.path("/inventory/**")
                        .filters(f->f.stripPrefix(1))
                        .uri("lb://inventory-service"))
                .route("music", r -> r.path("/music/**")
                        .filters(f->f.stripPrefix(1))
                        .uri("lb://music-service"))
                .route("order", r->r.path("/order/**")
                        .filters(f->f.stripPrefix(1))
                        .uri("lb://order-service"))
                .route("product",r -> r.path("/product/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://product-service"))
                .route("society", r -> r.path("/society/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://society-service"))
                .build();

    }
}
