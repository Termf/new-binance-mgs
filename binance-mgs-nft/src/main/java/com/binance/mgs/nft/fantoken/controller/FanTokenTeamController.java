package com.binance.mgs.nft.fantoken.controller;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.nft.bnbgtwservice.api.data.dto.FanTokenComplianceAssetDto;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.mgs.nft.fantoken.helper.CacheProperty;
import com.binance.mgs.nft.fantoken.helper.FanTokenCacheHelper;
import com.binance.mgs.nft.fantoken.helper.FanTokenCheckHelper;
import com.binance.mgs.nft.mysterybox.helper.FanTokenI18nHelper;
import com.binance.nft.fantoken.constant.CallerTypeEnum;
import com.binance.nft.fantoken.constant.TeamStatusEnum;
import com.binance.nft.fantoken.ifae.IFanTokenTeamManageAPI;
import com.binance.nft.fantoken.request.CommonPageRequest;
import com.binance.nft.fantoken.request.QueryTeamByPageRequest;
import com.binance.nft.fantoken.request.QueryTeamInfoRequest;
import com.binance.nft.fantoken.response.CommonPageResponse;
import com.binance.nft.fantoken.vo.TeamVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <h1>team</h1>
 * 考虑在灰度环境下不走 cache
 * Team 响应中增加 Collection、PowerStation 两个 Tab 的开关标志位
 * 2022.08.19 Team Revamp
 * */
@Slf4j
@RequestMapping("/v1/public/nft/fantoken")
@RestController
@RequiredArgsConstructor
public class FanTokenTeamController {

    private final CacheProperty cacheProperty;
    private final FanTokenCacheHelper fanTokenCacheHelper;

    private final IFanTokenTeamManageAPI fanTokenTeamManageAPI;
    private final BaseHelper baseHelper;
    private final FanTokenI18nHelper fanTokenI18nHelper;
    private final FanTokenCheckHelper fanTokenCheckHelper;

    /**
     * <h2>如果是在生产环境下, 对于灰度数据不要返回</h2>
     * */
    @GetMapping("/query-team-info")
    public CommonRet<TeamVO> queryTeamInfo(String symbol) throws Exception {

        boolean isGrey = fanTokenCheckHelper.isGray();
        TeamVO result;

        // use cache
        if (!isGrey && cacheProperty.isEnabled()) {
            log.info("query team info use cache: [{}]", symbol);
            result = fanTokenCacheHelper.queryTeamInfo(symbol);
            fanTokenI18nHelper.doTeamI18n(result);
        } else {
            log.info("query team info use db: [{}]", symbol);
            APIResponse<TeamVO> response = fanTokenTeamManageAPI.queryTeamInfo(APIRequest.instance(
                    QueryTeamInfoRequest.builder().symbol(symbol).callerType(CallerTypeEnum.CUSTOM.getCallerType())
                            .build()));
            baseHelper.checkResponse(response);
            fanTokenI18nHelper.doTeamI18n(response.getData());
            result = response.getData();
        }

        // 1. 如果是在生产环境下, 对于灰度数据不要返回
        // 2. 对于生产环境下: launchpad, After launchpad 状态也不要返回
        if (!isGrey && null != result) {
            boolean illegalStatus = result.getTeamStatus().equals(TeamStatusEnum.LAUNCHPAD.getTeamStatus())
                    || result.getTeamStatus().equals(TeamStatusEnum.AFTER_LAUNCHPAD.getTeamStatus());
            if (result.getIsGray() || illegalStatus) {
                return new CommonRet<>(new TeamVO());
            }
        }

        // GCC 合规校验 (只校验生产环境)
        if (!isGrey && null != result) {
            FanTokenComplianceAssetDto complianceAsset = fanTokenCheckHelper.fanTokenComplianceAsset(baseHelper.getUserId());
            if (null != complianceAsset && !complianceAsset.getPass()) {
                List<String> assets = complianceAsset.getAssets();
                // 如果不合规, 设置为空
                if (CollectionUtils.isEmpty(assets) || !assets.contains(result.getTeamToken())) {
                    result = new TeamVO();
                }
            }
        }

        return new CommonRet<>(result);
    }

    @PostMapping("/query-team-by-page")
    public CommonRet<CommonPageResponse<TeamVO>> queryTeamByPage(
            @Valid @RequestBody CommonPageRequest<QueryTeamByPageRequest> request) throws Exception {

        boolean isGrey = fanTokenCheckHelper.isGray();
        Long userId = baseHelper.getUserId();

        request.getParams().setUserId(userId);
        request.getParams().setComplianceAsset(fanTokenCheckHelper.fanTokenComplianceAsset(userId));
        request.getParams().setCallerType(CallerTypeEnum.CUSTOM.getCallerType());
        request.getParams().setIsGrey(isGrey);

        APIResponse<CommonPageResponse<TeamVO>> response =
                fanTokenTeamManageAPI.queryTeamByPage(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        for (TeamVO team : response.getData().getData()) {
            fanTokenI18nHelper.doTeamI18n(team);
        }

        // 生产环境下, 再次过滤下灰度 Team
        if (!isGrey) {
            List<TeamVO> data = new ArrayList<>(response.getData().getData().size());
            for (TeamVO team : response.getData().getData()) {
                // 生产环境 Team
                if (!team.getIsGray()) {
                    data.add(team);
                }
            }
            response.getData().setData(data);
        }

        return new CommonRet<>(response.getData());
    }

    private CommonRet<CommonPageResponse<TeamVO>> queryPageTeamUseCache(
            CommonPageRequest<QueryTeamByPageRequest> request) throws Exception {

        // 从 cache/db 中拿到全量的 Banner 数据, 再做排序
        CommonPageResponse<TeamVO> allTeamVos = fanTokenCacheHelper.queryTeam();
        if (null == allTeamVos || CollectionUtils.isEmpty(allTeamVos.getData())) {
            return new CommonRet<>(
                    CommonPageResponse.<TeamVO>builder().page(request.getPage()).size(request.getSize())
                            .total(0).data(Collections.emptyList()).build()
            );
        }

        // i18n
        allTeamVos.getData().forEach(fanTokenI18nHelper::doTeamI18n);

        // 按照 order by team_rank desc
        List<TeamVO> orderTeamVos = allTeamVos.getData().stream()
                .sorted(Comparator.comparing(TeamVO::getTeamRank).reversed()).collect(Collectors.toList());

        // 分页
        int page = Objects.isNull(request.getPage()) || request.getPage() <= 0 ? 1 : request.getPage();
        int size = Objects.isNull(request.getSize()) || request.getSize() <= 0 ? 10 : request.getSize();

        if ((page - 1) * size <= orderTeamVos.size()) {

            List<TeamVO> result = orderTeamVos.subList(
                    (page - 1) * size, Math.min(page * size, orderTeamVos.size())
            );
            return new CommonRet<>(
                    CommonPageResponse.<TeamVO>builder().page(request.getPage()).size(request.getSize())
                            .total(allTeamVos.getTotal()).data(result).build()
            );
        }

        return new CommonRet<>(
                CommonPageResponse.<TeamVO>builder().page(request.getPage()).size(request.getSize())
                        .total(allTeamVos.getTotal()).data(Collections.emptyList()).build()
        );
    }
}
