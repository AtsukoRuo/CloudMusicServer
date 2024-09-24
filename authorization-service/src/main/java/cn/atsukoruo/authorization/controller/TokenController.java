package cn.atsukoruo.authorization.controller;

import cn.atsukoruo.authorization.exception.ExpiredJwtException;
import cn.atsukoruo.authorization.exception.UserBannedException;
import cn.atsukoruo.authorization.exception.UserNotFoundException;
import cn.atsukoruo.authorization.service.TokenService;
import cn.atsukoruo.common.config.ErrorCodeConfig;
import cn.atsukoruo.authorization.exception.BannedRefreshTokenException;
import cn.atsukoruo.common.utils.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController("/token")
public class TokenController {
    private final TokenService tokenService;
    public TokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * 重新获取 Access Token
     */
    @GetMapping("/refresh")
    public Response<Object> refresh(
            @RequestHeader("cn-atsukoruo-refreshToken") String refreshToken) {
        String newAccessToken;
        try {
            newAccessToken =  tokenService.refreshAccessToken(refreshToken);
        } catch (ExpiredJwtException e) {
            log.info(e.toString());
            return Response.fail(ErrorCodeConfig.EXPIRED_REFRESH_TOKEN ,"Refresh Token 已过期");
        } catch (UserBannedException e) {
            log.info(e.toString());
            return Response.fail(ErrorCodeConfig.USER_BANNED ,"用户被封禁");
        } catch (UserNotFoundException e) {
            log.info(e.toString());
            return Response.fail(ErrorCodeConfig.USER_NOT_FOUND ,"未查询到用户");
        } catch (BannedRefreshTokenException e) {
            log.info(e.toString());
            return Response.fail(ErrorCodeConfig.BANNED_REFRESH_TOKEN, "Refresh Token 已被禁用");
        } catch (Exception e) {
            log.error(e.toString());
            return Response.fail("遇到未知异常");
        }
        return Response.success(newAccessToken);
    }
}
