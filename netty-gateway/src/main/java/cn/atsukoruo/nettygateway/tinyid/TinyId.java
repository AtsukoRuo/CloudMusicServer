package cn.atsukoruo.nettygateway.tinyid;

import org.springframework.web.client.RestClient;

public class TinyId {
    private final static RestClient restClient = RestClient.create();
    public static Long nextId() {
        TinyIdEntity result = restClient
                .get()
                .uri("http://114.116.220.197:30011/tinyid/id/nextId?bizType=websocket&token=0f673adf80504e2eaa552f5d791b644c")
                .retrieve()
                .body(TinyIdEntity.class);
        return result.getData().get(0);
    }
}