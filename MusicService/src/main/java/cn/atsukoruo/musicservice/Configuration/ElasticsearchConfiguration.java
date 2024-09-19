package cn.atsukoruo.musicservice.Configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class ElasticsearchConfiguration {
    @Bean
    public ElasticsearchClient buildClient() {
        HttpHost httpHosts = new HttpHost("122.9.7.252", 9201);
        RestClientBuilder builder = RestClient.builder(httpHosts);

        // Create the low-level client
        RestClient restClient = builder.build();

        // Create the transport with a Jackson mapper
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());

        // 同步版本的客户端
        return new ElasticsearchClient(transport);
    }
}
