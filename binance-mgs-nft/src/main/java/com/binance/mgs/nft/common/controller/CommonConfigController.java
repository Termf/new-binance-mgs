package com.binance.mgs.nft.common.controller;

import com.binance.mgs.nft.core.config.MgsNftProperties;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class CommonConfigController {

    private final BaseHelper baseHelper;
    private final MgsNftProperties mgsNftProperties;

    @GetMapping("friendly/nft/common-config")
    public CommonRet<Map<String, Object>> commonConfig() {
        Long userId = baseHelper.getUserId();
        userId = Optional.ofNullable(userId).orElse(99L);
        Map<String, Object> data = new HashMap<>();
        if(Objects.nonNull(mgsNftProperties.getCommonConfig()) &&mgsNftProperties.getCommonConfig().containsKey("aggregatorAbPercent")) {
            Number number = (Number) mgsNftProperties.getCommonConfig().get("aggregatorAbPercent");
            data.put("aggregatorAbHit", (userId % 100) < number.intValue());
        }

        if(Objects.nonNull(mgsNftProperties.getCommonConfig()) &&mgsNftProperties.getCommonConfig().containsKey("sweepAbPercent")) {
            Number number = (Number) mgsNftProperties.getCommonConfig().get("sweepAbPercent");
            boolean hit = mgsNftProperties.getCommonWhitelist().contains(userId)
                    ||(userId % 100) < number.intValue();
            data.put("sweepAbHit", hit);
        }
        return new CommonRet<>(data);
    }
}
