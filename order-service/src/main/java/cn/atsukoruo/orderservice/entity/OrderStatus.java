package cn.atsukoruo.orderservice.entity;

import cn.atsukoruo.common.entity.BaseEnum;

public enum OrderStatus implements BaseEnum {
    Paying(0, "待支付"),
    Paid(1, "已经支付成功");

    private int code;
    private String desc;
    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public int code() {
        return code;
    }
}
