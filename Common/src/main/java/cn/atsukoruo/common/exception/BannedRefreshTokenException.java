package cn.atsukoruo.common.exception;

public class BannedRefreshTokenException extends Exception{
    public BannedRefreshTokenException(String message) {
        super(message);
    }
}
