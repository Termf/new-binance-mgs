package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author Men Huatao (alex.men@binance.com)
 * @date 2021/5/17
 */
@Data
@ApiModel
public class SubUserCountLevelConfigRet {
    @ApiModelProperty("类型 SPOT MARGIN FUTURE BLVT")
    private String type;
    @ApiModelProperty("VIP等级")
    private Integer tradeLevel;
    @ApiModelProperty("当前可开通的数量")
    private Integer canOfferCount;
    @ApiModelProperty("最大开通的数量")
    private Integer maxAvailableCount;
    @ApiModelProperty("子账户数量等级配置表")
    private List<SubUserCountConfigVo> configVos;

    @Data
    @ApiModel("VIP等级对应子账户数量配置")
    public static class SubUserCountConfigVo {
        @ApiModelProperty("配置类型")
        private String type;
        private Integer vip0;
        private Integer vip1;
        private Integer vip2;
        private Integer vip3;
        private Integer vip4;
        private Integer vip5;
        private Integer vip6;
        private Integer vip7;
        private Integer vip8;
        private Integer vip9;
    }
}
