package com.binance.mgs.account.account.vo;

import com.binance.account.vo.security.enums.BizSceneEnum;
import com.binance.platform.mgs.base.vo.CommonArg;
import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

/**
 * @Author: mingming.sheng
 * @Date: 2020/4/14 8:17 下午
 */
@Data
@ApiModel("获取用户认证列表")
@EqualsAndHashCode(callSuper = false)
public class GetPubVerificationTwoCheckListArg extends ValidateCodeArg {

    @ApiModelProperty("邮箱")
    @Length(max = 255)
    private String email;

    @ApiModelProperty(value = "mobileCode")
    @Length(max = 10)
    private String mobileCode;
    @ApiModelProperty(value = "mobile")
    @Length(max = 50)
    private String mobile;

    @ApiModelProperty("业务场景")
    private BizSceneEnum bizScene;

    @ApiModelProperty("用来续期登录态的token")
    private String refreshToken;
}
