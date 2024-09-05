package cn.atsukoruo.AuthorizationService.Exception;

public class PasswordNotCorrectedException extends Exception {
    public PasswordNotCorrectedException(String message) {
        super(message);
    }
}
