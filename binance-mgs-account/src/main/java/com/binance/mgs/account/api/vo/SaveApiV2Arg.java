package com.binance.mgs.account.api.vo;

import com.binance.mgs.account.account.vo.MultiCodeVerifyArg;
import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

/**
 * Created by Fei.Huang on 2018/8/13.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SaveApiV2Arg extends MultiCodeVerifyArg {
    private static final long serialVersionUID = -4178564548679595317L;

    @NotBlank
    @Length(max = 200)
    private String apiName;

    @Length(max = 200)
    private String info;

    @Length(max = 20)
    private String tag;

    @ApiModelProperty(value = "验证方式：gt 极验,reCAPTCHA 谷歌, 为空 阿里云", allowableValues = "gt,bCAPTCHA,reCAPTCHA")
    private String validateCodeType;

    @ApiModelProperty(value = "极验验证二次验证表单数据 chllenge")
    private String geetestChallenge;

    @ApiModelProperty(value = "极验验证二次验证表单数据 validate")
    private String geetestValidate;

    @ApiModelProperty(value = " 极验验证二次验证表单数据 seccode")
    private String geetestSecCode;

    @ApiModelProperty(value = "极验验证 服务器端缓存的id，验证时需要回传，web端不需要，会从cookie中直接获取")
    private String gtId;

    @ApiModelProperty(value = "谷歌验证")
    private String recaptchaResponse;

    @ApiModelProperty(value = "谷歌验证sitekey")
    private String siteKey;

    @ApiModelProperty(value = "bCAPTCHA验证码token")
    private String bCaptchaToken;

    @ApiModelProperty(value = "安全验证sessionId")
    private String sessionId;

    @ApiModelProperty("RSA类型publicKey")
    private String publicKey;

    @ApiModelProperty("是否是前端创建")
    private Boolean isFromFE = Boolean.FALSE;
}

