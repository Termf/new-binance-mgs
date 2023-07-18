package com.binance.mgs.account.service;

import com.binance.accountanalyze.api.security.SecurityApi;
import com.binance.accountanalyze.vo.SecurityAssertionRequest;
import com.binance.accountanalyze.vo.SecurityAssertionResponse;
import com.binance.accountanalyze.vo.SecurityAttestationRequest;
import com.binance.accountanalyze.vo.SecurityAttestationResponse;
import com.binance.accountanalyze.vo.SecurityChallengeRequest;
import com.binance.accountanalyze.vo.SecurityChallengeResponse;
import com.binance.accountmonitorcenter.event.MetricsEventPublisher;
import com.binance.accountmonitorcenter.event.metrics.GenericMetricsEvent;
import com.binance.master.enums.TerminalEnum;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.authcenter.vo.AppAttestPrecheckRet;
import com.binance.mgs.account.security.vo.SecurityAppAttestCheckArg;
import com.binance.mgs.account.security.vo.SecurityAppAttestationArg;
import com.binance.mgs.account.security.vo.SecurityAppAttestationAssertionArg;
import com.binance.mgs.account.util.VersionUtil;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.google.common.collect.ImmutableMap;
import io.micrometer.core.instrument.Meter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
public class SecurityAppAttestService {
    private static final AppAttestPrecheckRet PRECHECK_EMPTY_RESPONSE = new AppAttestPrecheckRet();
    private static final String APP_ATTEST_VALIDATION_TYPE = "attest";

    @Value("${app.attest.enabled:false}")
    private boolean appAttestEnabled;
    @Value("#{'${app.attest.supported.client.types:ios}'.split(',')}")
    private Set<String> supportedClientTypes;
    @Value("${app.attest.supported.ios.version:}")
    private String supportedIosVersion;
    @Value("${app.attest.assertion.error.fail.enabled:false}")
    private boolean assertionErrorFailEnabled;
    @Value("${app.attest.assertion.result.action.enabled:false}")
    private boolean assertionActionEnabled;

    @Autowired
    private SecurityApi securityApi;
    @Autowired
    private BaseHelper baseHelper;
    @Autowired
    private MetricsEventPublisher metricsEventPublisher;

    public AppAttestPrecheckRet securityPreCheck(String attestKeyId) {
        if (!appAttestEnabled) {
            return PRECHECK_EMPTY_RESPONSE;
        }

        final String clientType = WebUtils.getClientType();
        if (!isClientTypeValid(clientType)) {
            log.debug("Client type [{}] is not supported", clientType);
            return PRECHECK_EMPTY_RESPONSE;
        }

        final String version = getVersion();
        if (!isVersionValid(version)) {
            log.debug("Version [{}] is not supported", version);
            return PRECHECK_EMPTY_RESPONSE;
        }

        final String fvideoId = CommonUserDeviceHelper.getFVideoId(WebUtils.getHttpServletRequest());
        if (StringUtils.isBlank(fvideoId)) {
            log.debug("fvideoId is empty [{}]", fvideoId);
            return PRECHECK_EMPTY_RESPONSE;
        }

        if (StringUtils.isBlank(attestKeyId)) {
            log.debug("ClientType [{}], version [{}], but attestKeyId is not passed [{}]",
                    clientType, version, attestKeyId);
            publishGenerateChallengeMetric(false, false, version);
            return PRECHECK_EMPTY_RESPONSE;
        }

        log.debug("Generating challenge for clientType [{}], version [{}], fvideoId [{}]", clientType, version, fvideoId);
        final String securityChallenge = getSecurityChallenge(fvideoId);
        if (StringUtils.isBlank(securityChallenge)) {
            log.warn("Failed to generate securityChallenge for fvideoId [{}]. Generated challenge is [{}]",
                    fvideoId, securityChallenge);
            publishGenerateChallengeMetric(true, false, version);
            return PRECHECK_EMPTY_RESPONSE;
        }

        log.debug("Successfully generated challenge for clientType [{}], version [{}], fvideoId [{}]. Challenge [{}]",
                clientType, version, fvideoId, securityChallenge);
        publishGenerateChallengeMetric(true, true, version);
        return createAppAttestPrecheckResponse(securityChallenge);
    }

    public String getSecurityChallenge(String deviceId) {
        log.trace("Executing SecurityAPI.getChallenge for deviceId [{}]", deviceId);
        final APIResponse<SecurityChallengeResponse> challengeResponse = executeGetSecurityChallenge(deviceId);
        log.trace("Response from SecurityAPI.getChallenge [{}] for deviceId [{}]", challengeResponse, deviceId);
        if (challengeResponse == null) {
            return null;
        }
        baseHelper.checkResponse(challengeResponse);
        if (challengeResponse.getData() == null || StringUtils.isBlank(challengeResponse.getData().getChallenge())) {
            log.warn("Empty challenge returned for device [{}]", deviceId);
            return null;
        }
        return challengeResponse.getData().getChallenge();
    }

    public boolean storeAttestation(SecurityAppAttestationArg args) {
        log.trace("Executing storeAttestation with args [{}]", args);
        try {
            APIResponse<SecurityAttestationResponse> attestationResponse = executeStoreAttestationValid(args);
            log.debug("Response from storeAttestation [{}] for args [{}]", attestationResponse, args);
            checkResponse(args, attestationResponse);
            final boolean valid = attestationResponse.getData().isValid();
            publishCheckAttestationMetric(valid, null);
            return valid;
        } catch (RuntimeException e) {
            publishCheckAttestationMetric(false, e.getMessage());
            return false;
        }
    }

    public boolean isAssertionValid(SecurityAppAttestationAssertionArg args) {
        return doIsAssertionValid(args);
    }

    public CheckAssertionResult checkAndGetAssertionResult(SecurityAppAttestCheckArg arg) {
        boolean isPassAssertion = checkAssertion(arg);
        final CheckAssertionResult result = new CheckAssertionResult(isPassAssertion, assertionActionEnabled);
        publishCheckAssertionMetric(result);
        return result;
    }

    /**
     * 校验评估
     *
     * @param arg
     * @return
     */
    private boolean checkAssertion(SecurityAppAttestCheckArg arg) {
        final SecurityAppAttestationAssertionArg assertionArg = new SecurityAppAttestationAssertionArg();
        assertionArg.setAssertion(arg.getAssertion());
        try {
            return doIsAssertionValid(assertionArg);
        } catch (RuntimeException e) {
            log.error("Error while checking assertion", e);
            return !assertionErrorFailEnabled;
        }
    }

    private boolean doIsAssertionValid(SecurityAppAttestationAssertionArg args) {
        log.trace("Executing isAssertionValid with args [{}]", args);
        APIResponse<SecurityAssertionResponse> assertionResponse = executeIsAssertionValid(args);
        log.debug("Response from isAssertionValid [{}] for args [{}]", assertionResponse, args);
        checkResponse(args, assertionResponse);
        return assertionResponse.getData().isValid();
    }

    private <S, T> void checkResponse(S args, APIResponse<T> attestationResponse) {
        if (attestationResponse == null) {
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }
        baseHelper.checkResponse(attestationResponse);
        if (attestationResponse.getData() == null) {
            log.warn("Empty response returned for args [{}]", args);
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }
    }

    private boolean isClientTypeValid(String clientType) {
        return StringUtils.isNotBlank(clientType) &&
                CollectionUtils.isNotEmpty(supportedClientTypes) &&
                supportedClientTypes.contains(clientType);
    }

    private boolean isVersionValid(String version) {
        return StringUtils.isNotBlank(version) &&
                StringUtils.isNotBlank(supportedIosVersion) &&
                VersionUtil.higherOrEqual(version, supportedIosVersion);
    }

    private APIResponse<SecurityChallengeResponse> executeGetSecurityChallenge(String deviceId) {
        try {
            return securityApi.getChallenge(APIRequest.instance(new SecurityChallengeRequest(deviceId)));
        } catch (BusinessException e) {
            log.error("Business Exception [{}] / [{}] while executing getChallenge for deviceId [{}]",
                    e.getBizCode(), e.getBizMessage(), deviceId, e);
        } catch (RuntimeException e) {
            log.error("General error while executing getChallenge for device [{}]", deviceId, e);
        }
        return null;
    }

    private APIResponse<SecurityAttestationResponse> executeStoreAttestationValid(SecurityAppAttestationArg args) {
        try {
            return securityApi.isAttestationValid(APIRequest.instance(new SecurityAttestationRequest(args.getAttestation())));
        } catch (BusinessException e) {
            log.error("Business Exception [{}] / [{}] while executing isAttestationValid with args [{}]",
                    e.getBizCode(), e.getBizMessage(), args, e);
            throw e;
        } catch (RuntimeException e) {
            log.error("General error while executing isAttestationValid with args [{}]", args, e);
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }
    }

    private APIResponse<SecurityAssertionResponse> executeIsAssertionValid(SecurityAppAttestationAssertionArg args) {
        try {
            return securityApi.isAssertionValid(APIRequest.instance(new SecurityAssertionRequest(args.getAssertion())));
        } catch (BusinessException e) {
            log.error("Business Exception [{}] / [{}] while executing isAssertionValid with args [{}]",
                    e.getBizCode(), e.getBizMessage(), args, e);
            throw e;
        } catch (RuntimeException e) {
            log.error("General error while executing isAssertionValid with args [{}]", args, e);
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }
    }

    private void publishGenerateChallengeMetric(boolean keyIdPassed, boolean success, String version) {
        metricsEventPublisher.publish(new GenericMetricsEvent("account.app.attest.challenge.generate", Meter.Type.COUNTER,
                ImmutableMap.of(
                        "keyid", Boolean.toString(keyIdPassed),
                        "success", Boolean.toString(success),
                        "version", StringUtils.defaultString(version, "null"))
        ));
    }

    private void publishCheckAttestationMetric(boolean valid, String error) {
        metricsEventPublisher.publish(new GenericMetricsEvent("account.app.attest.attestation.check", Meter.Type.COUNTER,
                ImmutableMap.of(
                        "valid", Boolean.toString(valid),
                        "error", StringUtils.defaultString(error, "null"))
        ));
    }

    private void publishCheckAssertionMetric(CheckAssertionResult result) {
        metricsEventPublisher.publish(new GenericMetricsEvent("account.app.attest.assertion.check", Meter.Type.COUNTER,
                ImmutableMap.of(
                        "valid", Boolean.toString(result.isAssertionValid()),
                        "action", Boolean.toString(result.isAssertionAction()))
        ));
    }

    private static AppAttestPrecheckRet createAppAttestPrecheckResponse(String challenge) {
        final AppAttestPrecheckRet response = new AppAttestPrecheckRet();
        response.setChallenge(challenge);
        response.setValidationType(APP_ATTEST_VALIDATION_TYPE);
        return response;
    }

    private static String getVersion() {
        TerminalEnum terminal = WebUtils.getTerminal();
        if (terminal == null) {
            return null;
        }
        return VersionUtil.getVersion(terminal);
    }

    public static class CheckAssertionResult {
        private final boolean assertionValid;
        private final boolean assertionAction;

        public CheckAssertionResult(boolean assertionValid, boolean assertionAction) {
            this.assertionValid = assertionValid;
            this.assertionAction = assertionAction;
        }

        public boolean isAssertionValid() {
            return assertionValid;
        }

        public boolean isAssertionAction() {
            return assertionAction;
        }
    }
}
