package cn.atsukoruo.AuthorizationService.Controller;

import cn.atsukoruo.AuthorizationService.Entity.User;
import cn.atsukoruo.AuthorizationService.Exception.DuplicateUserException;
import cn.atsukoruo.AuthorizationService.Exception.RegMatchException;
import cn.atsukoruo.AuthorizationService.Service.RegisterService;
import cn.atsukoruo.common.config.ErrorCodeConfig;
import cn.atsukoruo.common.utils.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
public class RegisterController {
    private final RegisterService registerService;

    public RegisterController(RegisterService registerService) {
        this.registerService = registerService;
    }

    @PostMapping("/register")
    public Response<Object> register(
        @RequestPart("username") String username,
        @RequestPart("nickname") String nickname,
        @RequestPart("password") String password,
        @RequestPart("avatar") MultipartFile file
    ) {
        log.debug("用户 " +  username + "正在注册");
        User newUser;
        try {
            newUser = registerService.register(username, nickname, password, file);
        } catch (DuplicateUserException e) {
            log.info(e.toString());
            return Response.fail(ErrorCodeConfig.DUPLICATED_USER, "用户重复");
        } catch (RegMatchException e) {
            log.error(e.toString());
            return Response.fail(ErrorCodeConfig.REG_MATCH_ERROR, "正则表达式匹配错误");
        } catch (Exception e) {
            Throwable cause =  e.getCause();
            log.error(e + "\ncause: " + (cause == null ? "" : cause.toString()));
            return Response.fail(ErrorCodeConfig.UNKNOWN_ERROR,
                    "遇到未知错误");
        }
        return Response.success(newUser);
    }

}
