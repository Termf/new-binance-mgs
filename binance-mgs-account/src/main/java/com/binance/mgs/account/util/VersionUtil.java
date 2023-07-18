package com.binance.mgs.account.util;

import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import com.binance.master.enums.TerminalEnum;
import com.binance.mgs.account.account.helper.VersionHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;

import com.binance.master.utils.WebUtils;
import com.binance.master.utils.version.ComparableVersion;
import com.github.zafarkhaja.semver.Version;

/**
 * 版本号小助手
 **/
public class VersionUtil {

    public static final String VERSION = "versionCode";
    public static final String VERSION_NAME = "versionName";
    public static final String BNC_APP_MODE = "BNC-App-Mode";


    public static final String ANDROID_VERSION = "versioncode";
    public static final String ANDROID_VERSION_NAME = "versionname";


    public static final String ELECTRON_VERSION_NAME = "versionname";


    /**
     * @param srcVersionStr    src版本
     * @param targetVersionStr target版本
     * @return 1：src版本高于target 0：版本相同 -1：版本落后
     */
    public static int compare(String srcVersionStr, String targetVersionStr) {
        ComparableVersion srcVersion = new ComparableVersion(srcVersionStr);
        ComparableVersion targetVersion = new ComparableVersion(targetVersionStr);

        return srcVersion.compareTo(targetVersion);
    }

    public static boolean higher(String srcVersionStr, String targetVersionStr) {
        return compare(srcVersionStr, targetVersionStr) == 1;
    }

    public static boolean higherOrEqual(String srcVersionStr, String targetVersionStr) {
        return compare(srcVersionStr, targetVersionStr) >= 0;
    }

    public static boolean lower(String srcVersionStr, String targetVersionStr) {
        return compare(srcVersionStr, targetVersionStr) == -1;
    }

    public static boolean equals(String srcVersionStr, String targetVersionStr) {
        return compare(srcVersionStr, targetVersionStr) == -1;
    }

    /**
     * use {@link VersionHelper#getVersion()} replace
     */
    @Deprecated
    public static String getVersion() {
        HttpServletRequest request = WebUtils.getHttpServletRequest();
        if (Objects.nonNull(request)) {
            String versionCode = request.getParameter(VERSION);
            if (StringUtils.isBlank(versionCode)) {
                versionCode = request.getHeader(VERSION);
            }
            return versionCode;
        } else {
            return Strings.EMPTY;
        }
    }

    /**
     * use {@link VersionHelper#getVersion()} replace
     */
    @Deprecated
    public static String getVersionName() {
        HttpServletRequest request = WebUtils.getHttpServletRequest();
        String versionName = request.getParameter(VERSION_NAME);
        if (StringUtils.isBlank(versionName)) {
            versionName = request.getHeader(VERSION_NAME);
        }
        return versionName;
    }

    public static boolean lowerVersionName(String compareVersionName, String currentVersionName) {
        Version currentVersion = Version.valueOf(currentVersionName);
        Version compareVersion = Version.valueOf(compareVersionName);
        return currentVersion.lessThan(compareVersion);
    }

    public static String getBncAppMode() {
        HttpServletRequest request = WebUtils.getHttpServletRequest();
        if (Objects.nonNull(request)) {
            String versionCode = request.getParameter(BNC_APP_MODE);
            if (StringUtils.isBlank(versionCode)) {
                versionCode = request.getHeader(BNC_APP_MODE);
            }
            return versionCode;
        } else {
            return Strings.EMPTY;
        }
    }


    public static String getVersion(TerminalEnum terminal) {
        if (terminal == null) {
            return null;
        }
        HttpServletRequest request = WebUtils.getHttpServletRequest();
        switch (terminal) {
            case ANDROID:
                String versionName = request.getParameter(ANDROID_VERSION_NAME);
                if (StringUtils.isBlank(versionName)) {
                    versionName = request.getHeader(ANDROID_VERSION_NAME);
                }
                return versionName;
            case IOS:
                String versionCode = request.getParameter(VERSION);
                if (StringUtils.isBlank(versionCode)) {
                    versionCode = request.getHeader(VERSION);
                }
                return versionCode;
            case ELECTRON:
                String electronVersionName = request.getParameter(ELECTRON_VERSION_NAME);
                if (StringUtils.isBlank(electronVersionName)) {
                    electronVersionName = request.getHeader(ELECTRON_VERSION_NAME);
                }
                return electronVersionName;
            default:
                return null;
        }
    }
}
