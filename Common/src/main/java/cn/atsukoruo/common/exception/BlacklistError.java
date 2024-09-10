package cn.atsukoruo.common.exception;

public class BlacklistError extends RuntimeException {
    public BlacklistError() {
        super();
    }
    public BlacklistError(String message) {
        super(message);
    }
}
