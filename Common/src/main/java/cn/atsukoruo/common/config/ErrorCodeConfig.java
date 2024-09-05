package cn.atsukoruo.common.config;

public class ErrorCodeConfig {
    public static final int UNKNOWN_ERROR = 0x00000000;
    public static final int USER_NOT_FOUND = 0x00000001;
    public static final int PASSWORD_NOT_CORRECTED = 0x00000002;
    public static final int USER_BANNED = 0x000000003;
    public static final int DUPLICATED_USER = 0x00000004;
    public static final int REG_MATCH_ERROR = 0x00000005;

    public static final int EXPIRED_JWT = 0x00000006;

    public static final int BANNED_REFRESH_TOKEN = 0x00000007;
    public static final int BANNED_ACCESS_TOKEN = 0x00000008;
}
