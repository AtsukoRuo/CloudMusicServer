package cn.atsukoruo.AuthorizationService.Exception;

public class ExpiredJwtException extends Exception{
    public ExpiredJwtException(String message) {
        super(message);
    }
}
