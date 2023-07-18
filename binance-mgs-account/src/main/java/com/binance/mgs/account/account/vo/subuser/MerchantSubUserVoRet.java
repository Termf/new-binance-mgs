package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
public class MerchantSubUserVoRet {
	
	@ApiModelProperty("子账户UserId")
    private Long userId;

    @ApiModelProperty("子账户邮箱")
    private String email;

	@ApiModelProperty("备注")
	private String remark;

	@ApiModelProperty("创建时间")
	private Date insertTime;

	@ApiModelProperty("子账号类型 参考：com.binance.mgs.account.account.enums.SubUserBizType")
	private String subUserBizType;

	@ApiModelProperty("子账户开启状态")
	private Boolean isSubUserEnabled;

	@ApiModelProperty("子账户创建的最大个数")
	private Long maxSubUserNum;
}
