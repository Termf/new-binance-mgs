package com.binance.mgs.account.ddos;

public enum DdosOperationEnum {
    ACTIVE("ACTIVE", "激活场景"),
    APP_RESET_PASSWORD("APP_RESET_PASSWORD", "app reset pwd"),
    ONE_BUTTON_REGISTER("ONE_BUTTON_REGISTER", "ONE_BUTTON_REGISTER"),
    WEB_RESET_PASSWORD("WEB_RESET_PASSWORD", "web reset pwd"),
    DEVICE_AUTH("DEVICE_AUTH", "新设备授权"),

    ;

    private String key;
    private String desc;


    private DdosOperationEnum(String key, String desc) {
        this.key = key;
        this.desc = desc;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
