package cn.atsukoruo.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.json.JsonParseException;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JsonUtils {

    public static final ObjectMapper objectMapper =  new ObjectMapper();

    /**
     * json 转 Map
     * @param json
     * @return
     */
    public static Map<String, Object> parseObject(String json) {
        if (StringUtils.hasText(json)) {
            return parseObject(json, Map.class);
        }
        return new HashMap<>();
    }

    public static <T> T parseObject(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (IOException e) {
            throw new JsonParseException(e);
        }
    }

    public static String toJson(Object obj) {
        if (obj == "") {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new JsonParseException(e);
        }
    }
}