package com.binance.mgs.account.account.vo.enterprise;

import com.binance.accountsubuser.vo.enterprise.EnterpriseRoleVo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author dana.d
 */
@Data
@ApiModel
public class EnterpriseRoleAcctRet {
    @ApiModelProperty("角色账户UserId")
    private Long userId;
    @ApiModelProperty("角色账户邮箱")
    private String email;
    @ApiModelProperty("备注")
    private String remark;
    @ApiModelProperty("创建时间")
    private Date insertTime;
    @ApiModelProperty("角色列表")
    private List<EnterpriseRoleVo> enterpriseRoleVos;
    @ApiModelProperty("子账号类型")
    private String subUserBizType;
    @ApiModelProperty("子账户开启状态")
    private Boolean isSubUserEnabled;
}

