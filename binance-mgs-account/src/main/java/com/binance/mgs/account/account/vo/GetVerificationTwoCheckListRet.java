package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @Author: mingming.sheng
 * @Date: 2020/4/14 8:17 下午
 */
@Data
@ApiModel("获取2fa验证/绑定列表")
public class GetVerificationTwoCheckListRet implements Serializable {
    private static final long serialVersionUID = 4911029914067474678L;

    private Set<VerificationTwoBind> needBindVerifyList = new HashSet<>();

    private Set<VerificationTwoCheck> needCheckVerifyList = new HashSet<>();

    @ApiModelProperty("requestId")
    private String requestId;

    @ApiModelProperty("last2fa")
    private String last2fa;

    @Data
    public static class VerificationTwoCheck {
        private String verifyType;
        private String verifyTargetMask;
        private Integer verifyOption;// 验证是否可选（1-必选，0-可选）

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VerificationTwoCheck check = (VerificationTwoCheck) o;
            return verifyType.equals(check.verifyType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(verifyType);
        }
    }

    @Data
    public static class VerificationTwoBind {
        private String verifyType;
        private Integer bindOption;// 绑定是否可选（1-必选，0-可选）

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VerificationTwoBind that = (VerificationTwoBind) o;
            return verifyType.equals(that.verifyType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(verifyType);
        }
    }
}
