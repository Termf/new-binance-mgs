package com.binance.mgs.nft.fantoken.controller;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.mgs.nft.fantoken.helper.FanTokenCheckHelper;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.mgs.nft.fantoken.helper.FanTokenCacheHelper;
import com.binance.mgs.nft.mysterybox.helper.FanTokenI18nHelper;
import com.binance.nft.fantoken.ifae.IFanTokenBannerManageAPI;
import com.binance.nft.fantoken.request.CommonPageRequest;
import com.binance.nft.fantoken.request.QueryBannerByPageRequest;
import com.binance.nft.fantoken.response.CommonPageResponse;
import com.binance.nft.fantoken.vo.BannerVO;
import com.binance.nft.fantoken.vo.LanguageLinkInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RequestMapping("/v1/public/nft/fantoken")
@RestController
@RequiredArgsConstructor
public class FanTokenBannerController {

    private final FanTokenCacheHelper fanTokenCacheHelper;

    private final IFanTokenBannerManageAPI fanTokenBannerManageAPI;
    private final BaseHelper baseHelper;
    private final FanTokenI18nHelper fanTokenI18nHelper;
    private final FanTokenCheckHelper fanTokenCheckHelper;

    /**
     * <h2>mainpage: banner page</h2>
     * */
    @PostMapping("/query-banner-by-page")
    public CommonRet<CommonPageResponse<BannerVO>> queryBannerByPage(
            @Valid @RequestBody CommonPageRequest<QueryBannerByPageRequest> request) throws Exception {

        boolean isGray = fanTokenCheckHelper.isGray();
        Long userId = baseHelper.getUserId();
        request.getParams().setIsGray(isGray);
        request.getParams().setUserId(userId);
        request.getParams().setComplianceAsset(fanTokenCheckHelper.fanTokenComplianceAsset(userId));

        APIResponse<CommonPageResponse<BannerVO>> response =
                fanTokenBannerManageAPI.queryBannerByPage(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        for (BannerVO banner : response.getData().getData()) {
            fanTokenI18nHelper.doBannerI18n(banner);
        }
        parseMultiLanguageMediaLinks(response.getData().getData());
        return new CommonRet<>(response.getData());
    }

    private CommonRet<CommonPageResponse<BannerVO>> getBannerFromCache(
            CommonPageRequest<QueryBannerByPageRequest> request) throws Exception {

        // 从 cache/db 中拿到全量的 Banner 数据, 再做排序
        CommonPageResponse<BannerVO> allBannerVos = fanTokenCacheHelper.queryBanner();

        if (null == allBannerVos || CollectionUtils.isEmpty(allBannerVos.getData())) {
            return new CommonRet<>(
                    CommonPageResponse.<BannerVO>builder().page(request.getPage()).size(request.getSize())
                            .total(0).data(Collections.emptyList()).build()
            );
        }

        // i18n
        allBannerVos.getData().forEach(fanTokenI18nHelper::doBannerI18n);

        // 按照 order by banner_rank desc
        List<BannerVO> orderBannerVos = allBannerVos.getData().stream()
                .sorted(Comparator.comparing(BannerVO::getBannerRank).reversed()).collect(Collectors.toList());

        // 分页
        int page = Objects.isNull(request.getPage()) || request.getPage() <= 0 ? 1 : request.getPage();
        int size = Objects.isNull(request.getSize()) || request.getSize() <= 0 ? 10 : request.getSize();

        if ((page - 1) * size <= orderBannerVos.size()) {

            // 返回之前需要对 multiMediaLink 做语言适配
            List<BannerVO> result = orderBannerVos.subList(
                    (page - 1) * size, Math.min(page * size, orderBannerVos.size())
            );

            // 这里存在对 Guava Cache 的修改, 需要先将 result 重新拷贝一份, 再做处理
            List<BannerVO> target = new ArrayList<>(result.size());
            result.forEach(r -> target.add(CopyBeanUtils.fastCopy(r, BannerVO.class)));
            parseMultiLanguageMediaLinks(target);

            return new CommonRet<>(
                    CommonPageResponse.<BannerVO>builder().page(request.getPage()).size(request.getSize())
                            .total(allBannerVos.getTotal()).data(target).build()
            );
        }

        return new CommonRet<>(
                CommonPageResponse.<BannerVO>builder().page(request.getPage()).size(request.getSize())
                        .total(allBannerVos.getTotal()).data(Collections.emptyList()).build()
        );
    }

    /**
     * <h2>对 multiMediaLink 字段按照语言进行填充</h2>
     * */
    private void parseMultiLanguageMediaLinks(List<BannerVO> bannerVOS) {

        String language = baseHelper.getLanguage();
        bannerVOS.forEach(b -> {
            if (CollectionUtils.isNotEmpty(b.getMultiLanguageMediaLinks())) {
                // 兜底的 web link
                String defaultLink = b.getMultiLanguageMediaLinks().get(0).getLink();
                String matchedLink = matchedLanguageMediaLink(b.getMultiLanguageMediaLinks(), language);
                b.setMultiMediaLink(matchedLink == null ? defaultLink : matchedLink);
                b.setMultiLanguageMediaLinks(Collections.emptyList());  // 获取了 link 之后设置为空数组
            }
            if (CollectionUtils.isNotEmpty(b.getMobileMultiLanguageMediaLinks())) {
                // 兜底的 mobile link
                String defaultMobileLink = b.getMobileMultiLanguageMediaLinks().get(0).getLink();
                String matchedMobileLink = matchedLanguageMediaLink(b.getMobileMultiLanguageMediaLinks(), language);
                b.setMobileMultiMediaLink(matchedMobileLink == null ? defaultMobileLink : matchedMobileLink);
                b.setMobileMultiLanguageMediaLinks(Collections.emptyList());  // 获取了 link 之后设置为空数组
            }
        });
    }

    /**
     * <h2>获取匹配语言配置的 mediaLink</h2>
     * 没有匹配的会返回 null
     * */
    private String matchedLanguageMediaLink(List<LanguageLinkInfoVO> linkInfoVOS, String language) {

        String matchedLink = null;

        for (LanguageLinkInfoVO linkInfoVO : linkInfoVOS) {
            if (Arrays.asList(linkInfoVO.getLanguage().split(",")).contains(language)) {
                matchedLink = linkInfoVO.getLink();
                log.info("get matched language media link: [{}], [{}]", language, matchedLink);
                break;
            }
        }

        return matchedLink;
    }
}
