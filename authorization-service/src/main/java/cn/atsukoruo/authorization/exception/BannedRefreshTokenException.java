package cn.atsukoruo.authorization.exception;

public class BannedRefreshTokenException extends RuntimeException {
    public BannedRefreshTokenException(String message) {
        super(message);
    }
}
