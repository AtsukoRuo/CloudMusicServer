package cn.atsukoruo.societyservice;

import cn.atsukoruo.societyservice.Repository.PostMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

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
        Object msg = Map.of("from", 100, "to", 200, "relation", 1);
        ProducerRecord<String, String> pr = new ProducerRecord<>("relation", objectMapper.writeValueAsString(msg));
        kafkaTemplate.send(pr);
    }

    @Test
    public void receiveMessage() {
        consumer.subscribe(List.of("relation"));
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
        for (ConsumerRecord<String, String> record : records) {
            System.out.println("消费消息： " + record.value());
        }
    }

    @Autowired
    PostMapper postMapper;
    @Test
    public void copyToInbox() {

        postMapper.insertToInbox(1, 1, List.of(1, 2, 3));
    }
}
