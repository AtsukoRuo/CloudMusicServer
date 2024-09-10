package cn.atsukoruo.AuthorizationService.Exception;

public class PasswordNotCorrectedException extends RuntimeException {
    public PasswordNotCorrectedException(String message) {
        super(message);
    }
}
