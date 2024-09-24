package cn.atsukoruo.authorization.controller;
import cn.atsukoruo.authorization.exception.PasswordNotCorrectedException;
import cn.atsukoruo.authorization.exception.UserBannedException;
import cn.atsukoruo.authorization.exception.UserNotFoundException;
import cn.atsukoruo.authorization.service.LoginService;
import cn.atsukoruo.common.config.ErrorCodeConfig;
import cn.atsukoruo.common.utils.JsonUtils;
import cn.atsukoruo.common.utils.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@Slf4j
@RestController
public class LoginController {
    final private LoginService loginService;

    @Autowired
    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/login")
    public Response<Object> login(
            @RequestHeader("cn-atsukoruo-username") String username,
            @RequestHeader("cn-atsukoruo-password") String password,
            @RequestHeader("cn-atsukoruo-client") String client) {

        String[] tokens;
        String accessToken;
        String refreshToken;
        try {
            tokens = loginService.login(username, password, client);
            accessToken = tokens[0];
            refreshToken = tokens[1];
        } catch (UserNotFoundException e) {
            log.info(e.toString());
            return Response.fail(ErrorCodeConfig.USER_NOT_FOUND,
                    "账户不存在");
        } catch (PasswordNotCorrectedException e) {
            log.info(e.toString());
            return Response.fail(ErrorCodeConfig.PASSWORD_NOT_CORRECTED,
                    "密码错误");
        } catch (UserBannedException e) {
            log.info(e.toString());
            return Response.fail(ErrorCodeConfig.USER_BANNED,
                    "账户已被封禁");
        } catch (Exception e) {
            // 考虑到日志大小，只打印两层异常原因，这足够应对绝大多数场景了
            Throwable cause =  e.getCause();
            log.error(e + "\ncause: " + (cause == null ? "" : cause.toString()));
            return Response.fail(ErrorCodeConfig.UNKNOWN_ERROR,
                    "遇到未知错误");
        }

        return Response.success(
                JsonUtils.toJson(Map.of(
                        "accessToken", accessToken,
                        "refreshToken", refreshToken)));
    }

    // TODO 手机登录
}
