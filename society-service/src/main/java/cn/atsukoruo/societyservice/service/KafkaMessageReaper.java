package cn.atsukoruo.societyservice.service;

import cn.atsukoruo.common.utils.JsonUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class KafkaMessageReaper {

    private final ThreadPoolTaskExecutor executor;
    private final PostService postService;
    private final Consumer<String, String> postConsumer;
    private final KafkaTemplate<String, String> kafkaTemplate;
    public KafkaMessageReaper(
            @Qualifier("message-thread-pool") Executor threadPoolTaskExecutor,
            @Qualifier("post-consumer") Consumer<String, String> postConsumer,
            PostService postService,
            KafkaTemplate<String, String> kafkaTemplate) {
        this.executor = (ThreadPoolTaskExecutor) threadPoolTaskExecutor;
        this.postConsumer = postConsumer;
        this.postService = postService;
        this.kafkaTemplate = kafkaTemplate;
    }

    void handlePostMessage() {
        while(true) {
            try {
                ConsumerRecords<String, String> records =  postConsumer.poll(Duration.ofSeconds(10));
                for (ConsumerRecord<String, String> record : records) {
                    String value = record.value();
                    Map<String, Object> message = JsonUtils.parseObject(value);
                    Integer user = (Integer) message.get("user");
                    Integer post = (Integer) message.get("postId");
                    Integer timestamp = (Integer) message.get("timestamp");
                    executor.execute(() -> {
                        try {
                            postService.syncPublishPostIndexToFollowedUser(user, post, timestamp);
                        } catch (Exception e) {
                            log.error(e.toString());
                            // TODO 重新投递到重试队列中
                            kafkaTemplate.send("post", String.valueOf(user), value);
                        }
                    });
                    // TODO 这里应该维护一个滑动窗口的
                    postConsumer.commitSync();
                }
            } catch (Exception e) {
                log.error(e.toString());
                if (e instanceof IllegalStateException || e instanceof InterruptedException) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    @PostConstruct
    private void init() {
        executor.execute(this::handlePostMessage);
    }
}
