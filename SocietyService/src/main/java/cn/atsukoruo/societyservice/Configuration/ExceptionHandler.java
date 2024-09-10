package cn.atsukoruo.societyservice.Configuration;

import cn.atsukoruo.common.config.ErrorCodeConfig;
import cn.atsukoruo.common.utils.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.io.IOException;

@Slf4j
@ControllerAdvice
public class ExceptionHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public void handleException(
            HttpServletResponse response,
            Exception e
    ) throws IOException {
        log.error(e.toString());
        Response<Object> value = Response.fail(
                ErrorCodeConfig.UNKNOWN_ERROR,
                "捕获到未处理的异常：" + e.getClass().toString());;
        objectMapper.writeValue(response.getOutputStream(), value);
    }
}
