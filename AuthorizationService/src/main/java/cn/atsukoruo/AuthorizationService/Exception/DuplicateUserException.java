package cn.atsukoruo.AuthorizationService.Exception;

public class DuplicateUserException extends Exception {
    public DuplicateUserException(String message) {
        super(message);
    }
}
