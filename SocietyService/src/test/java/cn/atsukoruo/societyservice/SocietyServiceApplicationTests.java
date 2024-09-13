package cn.atsukoruo.societyservice;

import cn.atsukoruo.common.utils.JsonUtils;
import cn.atsukoruo.societyservice.Repository.PostMapper;
import cn.atsukoruo.societyservice.Service.RelationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatusCode;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@SpringBootTest
class SocietyServiceApplicationTests {

    @Test
    void contextLoads() {
    }

    @Autowired
    private Consumer<String, String> consumer;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    public void sendMessage() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Object msg = Map.of("from", 100, "to", 200);
        ProducerRecord<String, String> pr = new ProducerRecord<>("topic-demo", objectMapper.writeValueAsString(msg));
        kafkaTemplate.send(pr).join();
    }

    @Test
    public void receiveMessage() {
        consumer.subscribe(List.of("topic-demo"));
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(100));
        for (ConsumerRecord<String, String> record : records) {
            System.out.println("消费消息： " + record.value());
        }
        System.out.println("end");
    }

    @Autowired
    PostMapper postMapper;


    @Autowired
    private RestClient.Builder restClientBuilder;

    @Test
    public void testRestClient() {
        String result =  restClientBuilder.build().post().uri("http://authorization-service/login")
                .header("cn-atsukoruo-username", "atsukoruo")
                .header("cn-atsukoruo-password", "grf.2001")
                .header("cn-atsukoruo-client", "windows")
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, ((request, response) -> {}))
                .body(String.class);
        System.out.println(result);
    }

    @Autowired
    private RelationService relationService;
    @Test
    public void insertRelation() {
        for (int i = 0; i < 100; i++)
            relationService.follow(3 + i,  1);
    }

    @Test
    public void insertFollowedRelation() {
        for (int i = 0; i < 100; i++)
            relationService.follow(2,  3 + i);
    }

    @Test
    public void jsonList() {
        List<Integer> list = List.of(1, 2, 3 ,4);
        System.out.println(JsonUtils.toJson(list));
    }

    @Test
    public void jsonListString() {
        String json = "[1,2,3,4]";
        List<Integer> list =  JsonUtils.parseObject(json, List.class);
        System.out.println(list.toString());
        for (Integer i : list) {
            System.out.println(i);
        }
    }

    @Test
    public void getAllFollowedUser() {
        List<Integer>  users =  relationService.getFollowedUser(1, 0, Integer.MAX_VALUE);
        System.out.println(users);
    }
}
