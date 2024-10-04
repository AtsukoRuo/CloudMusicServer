package cn.atsukoruo.orderservice.service;

import cn.atsukoruo.common.entity.Pair;
import cn.atsukoruo.common.utils.JsonUtils;
import cn.atsukoruo.orderservice.configuration.XunhuPayConfig;
import cn.atsukoruo.orderservice.entity.Order;
import cn.atsukoruo.orderservice.entity.OrderStatus;
import cn.atsukoruo.orderservice.repository.OrderMapper;
import cn.hutool.crypto.SecureUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

@Slf4j
@Service
public class OrderService {
    private final OrderMapper orderMapper;
    private final RestClient restClient;
    private final XunhuPayConfig xunhuPayConfig;
    public OrderService(OrderMapper orderMapper,
                        XunhuPayConfig config,
                        RestClient restClient) {
        this.orderMapper = orderMapper;
        this.xunhuPayConfig = config;
        this.restClient = restClient;
    }

    @Transactional
    public Pair<Long, String> createOrder(
            Integer product,
            BigDecimal price,
            Integer amount,
            Integer user,
            String title) {
        Long orderId = buildOrderNumber(user);
        Order order = Order.builder()
                .orderNumber(orderId)
                .user(user)
                .product(product)
                .createTime(new Timestamp(System.currentTimeMillis()))
                .price(price)
                .status(OrderStatus.Paying)
                .amount(amount)
                .title(title)
                .build();

        String codeUrl = getCodeQr(order);
        if (codeUrl == null) {
            throw new RuntimeException("支付二维码生成失败");
        }
        try {
            orderMapper.createOrder(order);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return new Pair<>(orderId, codeUrl);
    }

    private String getCodeQr(Order order) {
        String total = order.getPrice().multiply(new BigDecimal(order.getAmount())).toString();
        String randomStr = UUID.randomUUID().toString().substring(0, 16);

        Map<String, String> params = new HashMap<>();
        params.put("appid", xunhuPayConfig.getAppid());
        params.put("version", xunhuPayConfig.getVersion());
        params.put("trade_order_id", order.getOrderNumber().toString());
        params.put("total_fee", total);
        params.put("title", order.getTitle());
        params.put("time", String.valueOf(System.currentTimeMillis()));
        params.put("notify_url", xunhuPayConfig.getNotifyUrl());
        params.put("nonce_str", randomStr);

        String hash = createSign(params, xunhuPayConfig.getKey());
        params.put("hash", hash);

         String result = restClient.post().uri(xunhuPayConfig.getPayUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .body(params)
                .retrieve()
                .body(String.class);

        Map<String, Object> data = JsonUtils.parseObject(result);
        log.info(data.toString());
        Integer errorCode = (Integer) data.get("errcode");
        String errMsg = (String) data.get("errmsg");
        if (errorCode ==0 && errMsg.equals("success!")) {
            return (String)data.get("url_qrcode");
        }
        return null;
    }

    private String createSign(Map<String, String> params, String privateKey) {
        String[] sortedKeys = params.keySet().toArray(new String[]{});
        Arrays.sort(sortedKeys);
        StringBuilder builder = new StringBuilder();
        for (String key : sortedKeys) {
            builder.append(key).append("=").append(params.get(key)).append("&");
        }
        String result = builder.deleteCharAt(builder.length() - 1).toString();
        String stringSignTemp = result + privateKey;
        return SecureUtil.md5(stringSignTemp);
    }

    private Long buildOrderNumber(Integer user) {
        int gen = user % 2;
        return (System.currentTimeMillis() << 2) | gen;
    }

    public List<Order> queryOrder(Integer user) {
        try {
            return orderMapper.queryOrdersByUser(user);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void payCallback(String tradeOrderId) {
        log.info(tradeOrderId);
        orderMapper.payOrder(tradeOrderId, OrderStatus.Paid);
    }
}
