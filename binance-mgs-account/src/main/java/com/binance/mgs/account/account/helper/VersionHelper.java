package com.binance.mgs.account.account.helper;

import com.binance.master.enums.TerminalEnum;
import com.binance.mgs.account.util.VersionUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.binance.platform.mgs.base.helper.BaseHelper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class VersionHelper extends BaseHelper {

    @Value("${versionUtil.use.terminal.param.switch:false}")
    private boolean versionUtilUseTerminalParamSwitch;

    /**
     * 判断某项新功能APP端是否可以访问
     * @param androidVersion 支持新功能的android版本，若为空则认为false不支持
     * @param iosVersion 同androidVersion
     * @return
     */
    public boolean checkAppVersion(String androidVersion, String iosVersion) {
        String currentVersion = getVersion();
        if (StringUtils.isBlank(currentVersion)) return true;

        TerminalEnum terminal = this.getTerminal();
        log.info("Current terminal is {} currentVersion is {}", terminal.getCode(), currentVersion);
        switch (terminal) {
            case ANDROID:
                return StringUtils.isNotBlank(androidVersion) && VersionUtil.higherOrEqual(currentVersion, androidVersion);
            case IOS:
                return StringUtils.isNotBlank(iosVersion) && VersionUtil.higherOrEqual(currentVersion, iosVersion);
            default:
                return true;
        }
    }

    public String getVersion() {
        if (versionUtilUseTerminalParamSwitch) {
            log.info("versionUtil use terminal branch ----> version: {}, terminal: {}", VersionUtil.getVersion(getTerminal()), getTerminal());
            return VersionUtil.getVersion(getTerminal());
        }
        log.info("versionUtil default branch ----> version: {}, terminal: {}", VersionUtil.getVersion(), getTerminal());
        return VersionUtil.getVersion();
    }

}
