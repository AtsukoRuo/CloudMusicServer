package cn.atsukoruo.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Bean;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Message {
    Integer command;
    Long channelId;
    Long dest;
    String payload;
}
