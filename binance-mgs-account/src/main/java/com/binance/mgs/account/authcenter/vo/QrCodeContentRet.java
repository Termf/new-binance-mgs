package com.binance.mgs.account.authcenter.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@ApiModel
@Data
@EqualsAndHashCode(callSuper = false)
public class QrCodeContentRet implements Serializable {
    private static final long serialVersionUID = 5960020914696325632L;
    @ApiModelProperty(required = true, notes = "二维码标识")
    private String qrCode;
    @ApiModelProperty(required = true, notes = "二维码标识,NEW:初始状态，SCAN:已扫码，CONFIRM：已确认登录，EXPIRED：已过期")
    private String status;
    @ApiModelProperty("类型：CONFIRM，DEEPLINK")
    private String actionType;
    @ApiModelProperty("confirm内容")
    private ConfirmContent confirmContent;
    @ApiModelProperty("deep link内容")
    private DeepLinkContent deepLinkContent;
    @ApiModelProperty("内容")
    private List<Map.Entry> extendInfo;
    @ApiModelProperty("是否跳转页面")
    private boolean enableUrlRedirect;
    @ApiModelProperty("是否跳转页面")
    private UrlContent urlContent;



    @Data
    public static class ConfirmContent {
        private String title;
        private String message = "";
        private String confirmText;
        private String cancelText;
    }

    @Data
    public static class DeepLinkContent {
        private String path;
    }

    @Data
    public static class UrlContent {
        private String path;
        private String domainPrefix;
    }
}
