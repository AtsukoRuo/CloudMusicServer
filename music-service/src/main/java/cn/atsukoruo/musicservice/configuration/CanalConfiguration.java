package cn.atsukoruo.musicservice.configuration;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

@Configuration
public class CanalConfiguration {
    CanalConnector canalConnector;

    @Bean("music-canal-connector")
    public CanalConnector buildCanalConnector() {
        String destination = "song";
        CanalConnector canalConnector = CanalConnectors.newSingleConnector(
                new InetSocketAddress("122.9.36.231", 11111),
                destination,
                "canal",
                "canal");

        canalConnector.connect();

        canalConnector.subscribe("cloud_music_song\\..*");
        canalConnector.rollback();
        this.canalConnector = canalConnector;
        return canalConnector;
    }
}
