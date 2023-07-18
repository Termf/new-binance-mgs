package com.binance.mgs.account.util;

import com.binance.account.vo.country.CountryVo;
import com.binance.accountmonitorcenter.event.MetricsEventPublisher;
import com.binance.accountmonitorcenter.event.metrics.constant.CallStatusEnum;
import com.binance.accountmonitorcenter.event.metrics.mgsaccount.PhoneNumberUtilsCounterMetrics;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.mgs.account.account.helper.AccountCountryHelper;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;


@Slf4j
@Component
public class PhoneNumberUtils {

    @Value("${phonenumber.check.switch:false}")
    private boolean phoneNumberCheckSwitch;

    @Value("${phonenumber.remove.zero.check.switch:true}")
    private boolean removeZeroCheck;

    @Value("#{'${phonenumber.check.weak.validate.country:BR}'.split(',')}")
    private List<String> weakValidateCountry;

    @Value("#{'${phonenumber.check.no.validate.country:}'.split(',')}")
    private List<String> noValidateCountry;

    // + 号开头的手机号校验国家
    @Value("#{'${phonenumber.check.plus.validate.country:DO,DO1,DO2}'.split(',')}")
    private List<String> plusValidateCountry;

    private static final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    @Autowired
    private AccountCountryHelper accountCountryHelper;

    @Autowired
    private MetricsEventPublisher metricsEventPublisher;

    /**
     *
     * @param mobileCode 两位字母国家码
     * @param mobile 手机号
     * @return
     */
    public void checkPhoneNumber(String mobileCode, String mobile) {
        if (!phoneNumberCheckSwitch) {
            return;
        }
        mobileCode = mobileCode.toUpperCase();

        if (noValidateCountry.contains(mobileCode)) {
            return;
        }

        log.info("start check PhoneNumberUtils.checkPhoneNumber");
        metricsEventPublisher.publish(PhoneNumberUtilsCounterMetrics.builder()
                .callStatus(CallStatusEnum.START)
                .mobileCode(mobileCode)
                .build()
        );

        boolean validPhoneNumber;

        boolean isWeakValidate = weakValidateCountry.contains(mobileCode);

        if (plusValidateCountry.contains(mobileCode)) {
            CountryVo countryVo = accountCountryHelper.getCountry(mobileCode);
            if (Objects.isNull(countryVo)) {
                return;
            }
            validPhoneNumber = PhoneNumberUtils.isValidPhoneNumberWithPlus(countryVo.getMobileCode(), mobile, isWeakValidate);
        } else {
            validPhoneNumber = PhoneNumberUtils.isValidPhoneNumber(mobileCode, mobile, isWeakValidate);
            if (removeZeroCheck && !validPhoneNumber && mobile.startsWith("0")) {
                // 如果手机号校验没过，并且手机号以 0 开头
                String withoutZeroMobile = mobile.replaceAll("^(0+)", "");
                validPhoneNumber = PhoneNumberUtils.isValidPhoneNumber(mobileCode, withoutZeroMobile, isWeakValidate);
            }
        }

        if (!validPhoneNumber) {
            log.info("checkPhoneNumber failed [countryCode:{} number:{} isWeakValidate:{}]", mobileCode, mobile, isWeakValidate);
            metricsEventPublisher.publish(PhoneNumberUtilsCounterMetrics.builder()
                    .weakValidate(String.valueOf(isWeakValidate))
                    .callStatus(CallStatusEnum.FAIL)
                    .mobileCode(mobileCode)
                    .build()
            );
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        metricsEventPublisher.publish(PhoneNumberUtilsCounterMetrics.builder()
                .weakValidate(String.valueOf(isWeakValidate))
                .callStatus(CallStatusEnum.SUCCESS)
                .mobileCode(mobileCode)
                .build()
        );
    }



    /**
     *
     * @param mobileCode 两位字母国家码
     * @param mobile 手机号
     * @return
     */
    public static boolean isValidPhoneNumber(String mobileCode, String mobile, boolean isPossible) {
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(mobile, mobileCode);
            if (isPossible) {
                return phoneNumberUtil.isPossibleNumber(number);
            }
            return phoneNumberUtil.isValidNumber(number);
        } catch (NumberParseException e) {
            log.info("parse number failed ", e);
            return false;
        }
    }

    /**
     *
     * @param countryCode 数字国家码
     * @param mobile 手机号
     * @param isPossible 校验强度 true 使用 isPossibleNumber
     * isPossibleNumber - 性能好，校验简单
     * isValidNumber - 合法性判断更完善
     * @return
     */
    private static boolean isValidPhoneNumberWithPlus(String countryCode, String mobile, boolean isPossible) {
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse("+" + countryCode + mobile, null);
            if (isPossible) {
                return phoneNumberUtil.isPossibleNumber(number);
            }
            return phoneNumberUtil.isValidNumber(number);
        } catch (NumberParseException e) {
            log.info("parse number failed ", e);
            return false;
        }
    }

}