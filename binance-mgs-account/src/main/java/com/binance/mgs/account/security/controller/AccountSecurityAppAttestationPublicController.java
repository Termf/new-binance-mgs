package com.binance.mgs.account.security.controller;

import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.mgs.account.advice.AccountDefenseResource;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.mgs.account.security.vo.SecurityAppAttestationArg;
import com.binance.mgs.account.security.vo.SecurityAppAttestationAssertionArg;
import com.binance.mgs.account.security.vo.SecurityAppAttestationAssertionRet;
import com.binance.mgs.account.security.vo.SecurityAppAttestationChallengeArg;
import com.binance.mgs.account.security.vo.SecurityAppAttestationChallengeRet;
import com.binance.mgs.account.security.vo.SecurityAppAttestationRet;
import com.binance.mgs.account.service.SecurityAppAttestService;
import com.binance.platform.mgs.base.BaseAction;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/v1/public/account/security/app/attest")
@Slf4j
public class AccountSecurityAppAttestationPublicController extends BaseAction {
    @Autowired
    private SecurityAppAttestService securityAppAttestService;

    @AccountDefenseResource(name = "AccountSecurityAppAttestationPublicController.getSecurityChallenge")
    @DDoSPreMonitor(action = "getAppAttestSecurityChallenge")
    @PostMapping(value = "/challenge")
    public CommonRet<SecurityAppAttestationChallengeRet> getSecurityChallenge(@RequestBody @Validated SecurityAppAttestationChallengeArg args) {
        final String challenge = securityAppAttestService.getSecurityChallenge(args.getDeviceId());
        if (StringUtils.isBlank(challenge)) {
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }
        SecurityAppAttestationChallengeRet response = new SecurityAppAttestationChallengeRet();
        response.setChallenge(challenge);
        return new CommonRet<>(response);
    }

    @AccountDefenseResource(name = "AccountSecurityAppAttestationPublicController.storeAttestation")
    @DDoSPreMonitor(action = "storeAttestation")
    @PostMapping(value = "/attestation")
    public CommonRet<SecurityAppAttestationRet> storeAttestation(@RequestBody @Validated SecurityAppAttestationArg args) {
        final boolean attestationValid = securityAppAttestService.storeAttestation(args);
        SecurityAppAttestationRet response = new SecurityAppAttestationRet();
        response.setValid(attestationValid);
        return new CommonRet<>(response);
    }

    @AccountDefenseResource(name = "AccountSecurityAppAttestationPublicController.isAssertionValid")
    @DDoSPreMonitor(action = "isAssertionValid")
    @PostMapping(value = "/assertion")
    public CommonRet<SecurityAppAttestationAssertionRet> isAssertionValid(@RequestBody @Validated SecurityAppAttestationAssertionArg args) {
        final boolean assertionValid = securityAppAttestService.isAssertionValid(args);
        SecurityAppAttestationAssertionRet response = new SecurityAppAttestationAssertionRet();
        response.setValid(assertionValid);
        return new CommonRet<>(response);
    }
}
