package cn.atsukoruo.societyservice.Configuration;

import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class KafkaConfiguration {
    // 创建 kafka 操作模板对象, 用于简化消息发送操作
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    // 创建 kafka 生产者工厂
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> properties = buildProducerProperties();
        return new DefaultKafkaProducerFactory<>(properties);
    }

    /**
     * 构建生产者配置
     */
    private static Map<String, Object> buildProducerProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "122.9.7.252:9092");
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.RETRIES_CONFIG, 3);
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        properties.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return properties;
    }


    @Bean("like-consumer")
    public Consumer<String, String> likeConsumer(ConsumerFactory<String, String> consumerFactory) {
        Consumer<String, String> consumer = consumerFactory.createConsumer();
        consumer.subscribe(List.of("like"));
        this.likeConsumer = consumer;
        return consumer;
    }


    /**
     * 创建 消费者对象
     */
    @Bean("post-consumer")
    public Consumer<String, String> consumer(ConsumerFactory<String, String> consumerFactory) {
        Consumer<String, String> consumer =  consumerFactory.createConsumer();
        consumer.subscribe(List.of("post"));
        this.postConsumer = consumer;
        return consumer;
    }

    @Bean
    public ConsumerFactory<String,String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(buildConsumerDefaultProperties());
    }

    /**
     * 构建消费者配置
     */
    private static Map<String, Object> buildConsumerDefaultProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "122.9.7.252:9092");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "post-consumer");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "60000");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return properties;
    }


    private Consumer<String, String> postConsumer;
    private Consumer<String, String> likeConsumer;

    @PreDestroy
    void destroyConsumer() {
        postConsumer.close();
        likeConsumer.close();
    }
}
