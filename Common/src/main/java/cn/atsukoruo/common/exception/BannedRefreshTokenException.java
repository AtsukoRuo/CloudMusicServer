package cn.atsukoruo.common.exception;

public class BannedRefreshTokenException extends RuntimeException {
    public BannedRefreshTokenException(String message) {
        super(message);
    }
}
