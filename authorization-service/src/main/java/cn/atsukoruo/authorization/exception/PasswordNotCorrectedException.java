package cn.atsukoruo.authorization.exception;

public class PasswordNotCorrectedException extends RuntimeException {
    public PasswordNotCorrectedException(String message) {
        super(message);
    }
}
