package cn.atsukoruo.common.config;

public class ErrorCodeConfig {
    public static final int UNKNOWN_ERROR =             0x00000000;
    public static final int USER_NOT_FOUND =            0x00000001;
    public static final int PASSWORD_NOT_CORRECTED =    0x00000002;
    public static final int USER_BANNED =               0x00000003;
    public static final int DUPLICATED_USER =           0x00000004;
    public static final int REG_MATCH_ERROR =           0x00000005;

    public static final int EXPIRED_REFRESH_TOKEN =     0x00000006;


    public static final int EXPIRED_ACCESS_TOKEN =      0x00000007;
    public static final int BANNED_REFRESH_TOKEN =      0x00000008;
    public static final int BANNED_ACCESS_TOKEN =       0x00000009;

    public static final int FAKE_TOKEN =                0x0000000A;
    public static final int BLACKLIST_ERROR =           0x0000000B;

    public static final int UNBIND_VX =                 0x0000000C;
    public static final int DUPLICATED_BIND_VX =        0x0000000D;
    public static final int OVERSELL_ERROR =            0x0000000E;
}
