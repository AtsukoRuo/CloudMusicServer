package cn.atsukoruo.common.utils;

import cn.atsukoruo.common.entity.BaseEnum;

public class EnumUtils {
    public static <T extends Enum<?> & BaseEnum> T codeOf(Class<T> enumClass, int code) {
        T[] enumConstants = enumClass.getEnumConstants();
        for (T t : enumConstants) {
            if (t.code() == code) {
                return t;
            }
        }
        return null;
    }
}
