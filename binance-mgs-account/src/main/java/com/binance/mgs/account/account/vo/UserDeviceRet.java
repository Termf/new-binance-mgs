package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class UserDeviceRet implements Serializable {

    private static final long serialVersionUID = -3633149265091365750L;

    /** 设备的ip地址 */
    public static final String LOGIN_IP = "login_ip";
    /** 客户端分辨率（Web端已PC客户端分辨率大小）长*宽 */
    public static final String SCREEN_RESOLUTION = "screen_resolution";
    /** 操作系统 / 以及版本号（系统版本号） */
    public static final String SYS_VERSION = "system_version";
    /** 设备品牌及型号 */
    public static final String BRANCH_MODEL = "brand_model";
    /** 设备名称 */
    public static final String DEVICE_NAME = "device_name";
    /** 系统语言 */
    public static final String SYS_LANG = "system_lang";
    /** 当前设备时区信息（Timezone） */
    public static final String TIMEZONE = "timezone";
    /** 动态设备ID */
    public static final String DEVICE_ID = "device_id";
    public static final String LOCATION_CITY = "location_city";

    private String id;

    private Long userId;

    private String agentType;

    private String source;

    private String deviceName;

    private String locationCity;

    private String loginIp;

    private Long loginTime;

    private Byte isDel;

    private String content;
    @ApiModelProperty("登录设备类型")
    private String clientType;
}
