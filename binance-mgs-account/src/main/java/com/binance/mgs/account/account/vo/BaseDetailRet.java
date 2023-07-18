package com.binance.mgs.account.account.vo;

import com.binance.account.vo.user.ex.OrderConfirmStatus;
import com.binance.account.vo.user.ex.UserSecurityKeyStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@ApiModel("获取用户详细信息")
public class BaseDetailRet implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 5170162742708277992L;

    @ApiModelProperty(value = "账号")
    private String email;
    @ApiModelProperty(value = "交易级别")
    private Integer tradeLevel;
    @ApiModelProperty(value = "推荐人")
    private Long agentId;

    @ApiModelProperty(value = "经纪人返佣比例")
    private BigDecimal agentRewardRatio;
    @ApiModelProperty(name = "被推荐人(当前userID)返佣比例")
    private BigDecimal referralRewardRatio;
    @ApiModelProperty(value = "买方交易手续费")
    private BigDecimal buyerCommission;
    @ApiModelProperty(value = "被动方手续费")
    private BigDecimal makerCommission;
    @ApiModelProperty(value = "卖方交易手续费")
    private BigDecimal sellerCommission;
    @ApiModelProperty(value = "主动方手续费")
    private BigDecimal takerCommission;

    @ApiModelProperty(value = "谷歌二次验证是否开启")
    private boolean gauth;
    @ApiModelProperty(value = "手机二次验证是否开启")
    private boolean mobileSecurity;
    @ApiModelProperty(value = "手机")
    private String mobileNo;
    @ApiModelProperty(value = "手机国家区号")
    private String mobileCode;
    @ApiModelProperty(value = "是否母账号")
    private boolean isParentUser;
    @ApiModelProperty(value = "是否子账号")
    private boolean isSubUser;
    @ApiModelProperty(value = "子账号是否已启用")
    private boolean isSubUserEnabled;
    @ApiModelProperty(value = "是否开启白名单提现")
    private boolean withdrawWhiteStatus;

    @ApiModelProperty(value = "BNB手续费开关是否开启")
    private Integer commissionStatus;
    @ApiModelProperty(value = "用户ID")
    private String userId;
    @ApiModelProperty(value = "限制APP交易开关")
    private String forbidAppTrade;
    @ApiModelProperty(value = "最后一次登录IP")
    private String lastLoginIp;
    @ApiModelProperty(value = "最后一次登录IP所在国家")
    private String lastLoginCountry;
    @ApiModelProperty(value = "最后一次登录IP所在城市")
    private String lastLoginCity;
    @ApiModelProperty(value = "最后一次登录时间")
    private Date lastLoginTime;
    @ApiModelProperty(value = "证件照片")
    private String idPhoto;
    @ApiModelProperty(value = "身份证审核被拒绝原因")
    private String idPhotoMsg;
    @ApiModelProperty(value = "firstName")
    private String firstName;
    @ApiModelProperty(name = "middleName")
    private String middleName;
    @ApiModelProperty(value = "lastName")
    private String lastName;
    @ApiModelProperty(value = "公司名称")
    private String companyName;
    @ApiModelProperty(value = "认证类型 1个人 2企业")
    private Integer authenticationType;
    @ApiModelProperty(value = "开启jumio身份认证 1:enable 0 disable")
    private Integer jumioEnable;
    @ApiModelProperty(value = "用户安全级别:1:普通,2:身份认证,3:?")
    private Integer level;
    @ApiModelProperty(value = "不同级别对应的提现额度")
    private List<BigDecimal> levelWithdraw;
    @ApiModelProperty(value = "认证地址信息")
    private String certificateAddress;
    @ApiModelProperty(value = "是否存在margin账号")
    private Boolean isExistMarginAccount;
    @ApiModelProperty("协议确定")
    private Boolean isUserProtocol;
    @ApiModelProperty(name = "Security Key status")
    private UserSecurityKeyStatus securityKeyStatus;
    @ApiModelProperty(name = "用户下单确认状态开关")
    private OrderConfirmStatus orderConfirmStatus;
    @ApiModelProperty(value = "是否存在future账号")
    private Boolean isExistFutureAccount;
    @ApiModelProperty(value = "是否提交过返佣设置的表格")
    private Boolean isReferralSettingSubmitted;
    @ApiModelProperty(name = "昵称")
    private String nickName;
    @ApiModelProperty(name = "昵称背景色")
    private String nickColor;
    @ApiModelProperty(value = "是否是资产子账号")
    private Boolean isAssetSubAccount;
    @ApiModelProperty(value = "是否存在fiat账号")
    private Boolean isExistFiatAccount;

    @ApiModelProperty(name = "是否拥有矿池账户")
    private Boolean isExistMiningAccount;

    @ApiModelProperty(name = "是否拥有card账户")
    private Boolean isExistCardAccount;

    @ApiModelProperty(name = "是否签署leverage token风险协议")
    private Boolean isSignedLVTRiskAgreement;

    @ApiModelProperty(name = "是否绑定邮箱")
    private Boolean isBindEmail;


    @ApiModelProperty(name = "是否是手机号注册用户")
    private Boolean isMobileUser;

    @ApiModelProperty(name = "是否开启站内转账（即快速提币 ）")
    private Boolean userFastWithdrawEnabled;


    @ApiModelProperty(value = "是否broker母账号")
    private boolean isBrokerParentUser;
    @ApiModelProperty(value = "是否broker子账号")
    private boolean isBrokerSubUser;
    @ApiModelProperty(readOnly = true, notes = "是否是无邮箱子账号")
    private Boolean isNoEmailSubUser;

    @ApiModelProperty(readOnly = true, notes = "托管账户功能是否开启")
    private Boolean isManagerSubUserFunctionEnabled;

    @ApiModelProperty(readOnly = true, notes = "是否开启批量新增提币白名单地址")
    private Boolean isAllowBatchAddWhiteAddress;

    @ApiModelProperty(readOnly = true, notes = "是否锁定提币白名单地址")
    private Boolean isLockWhiteAddress;

    @ApiModelProperty(readOnly = true, notes = "是否用户需要做kyc")
    private Boolean isUserNeedKyc;

    @ApiModelProperty(value = "是否是CommonMerchant子账号")
    private Boolean isCommonMerchantSubUser;

    @ApiModelProperty(value = "是否是企业角色子账号")
    private Boolean isEnterpriseRoleUser;

    @ApiModelProperty(readOnly = true, notes = "是否有Future/Delivery一键平仓权限")
    private Boolean isOneButtonClearPosition;

    @ApiModelProperty(readOnly = true, notes = "是否是统一账户")
    private Boolean isPortfolioMarginRetailUser;

    @ApiModelProperty(readOnly = true, notes = "是否有托管子账户Future/Delivery一键平仓权限")
    private Boolean isOneButtonManagerSubUserClearPosition;

    @ApiModelProperty(readOnly = true, notes = "是否个人或企业账户,默认0个人,1企业")
    private Boolean isUserPersonalOrEnterpriseAccount;

    @ApiModelProperty(readOnly = true, notes = "注册渠道")
    private String registerChannel;

    @ApiModelProperty(readOnly = true, notes = "是否拥有Option账户")
    private Boolean isExistOptionAccount;
}
