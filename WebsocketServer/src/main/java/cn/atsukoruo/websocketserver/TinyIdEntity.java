package cn.atsukoruo.websocketserver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TinyIdEntity {
    List<Long> data;
    Integer code;
    String message;
}
