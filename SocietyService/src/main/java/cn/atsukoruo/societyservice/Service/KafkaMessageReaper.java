package cn.atsukoruo.societyservice.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class KafkaMessageReaper {

    @Async
    public void receive() {
        while(true) {
            try {

            } finally {

            }
        }
    }

    @PostConstruct
    private void init() {
        receive();
    }
}
