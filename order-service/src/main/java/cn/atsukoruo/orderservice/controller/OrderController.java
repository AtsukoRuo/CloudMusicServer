package cn.atsukoruo.orderservice.controller;

import cn.atsukoruo.common.entity.Pair;
import cn.atsukoruo.common.utils.Response;
import cn.atsukoruo.orderservice.entity.Order;
import cn.atsukoruo.orderservice.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class OrderController {
    private final OrderService orderService;
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }


    @PostMapping("/order/create")
    public Response<Object> createOrder(
            @RequestParam("product") Integer product,
            @RequestParam("price")BigDecimal price,
            @RequestParam("amount") Integer amount,
            @RequestParam("title") String title) {
        Integer user = getUserFromAuth();
        Pair<Long, String> pair = orderService.createOrder(product, price, amount, user, title);
        return Response.success(Map.of("orderId", pair.first(), "codeQr", pair.second()));
    }

    @GetMapping("/order")
    public Response<Object> queryOrder() {
        Integer user = getUserFromAuth();
        List<Order> orderList = orderService.queryOrder(user);
        return Response.success(orderList);
    }

    @PostMapping("/order/pay")
    public Response<Object> payOrder() {
        return Response.success();
    }

    @PostMapping("/pay/success")
    public void payCallback(String trade_order_id) {
        orderService.payCallback(trade_order_id);
    }


    private Integer getUserFromAuth() {
        SecurityContext context = SecurityContextHolder.getContext();
        UsernamePasswordAuthenticationToken user = (UsernamePasswordAuthenticationToken) context.getAuthentication();
        return Integer.parseInt(user.getName());
    }

}
