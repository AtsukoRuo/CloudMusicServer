package cn.atsukoruo.AuthorizationService.Exception;

public class UserBannedException extends RuntimeException{
    public UserBannedException(String message) {
        super(message);
    }
}
