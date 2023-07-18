package com.binance.mgs.account.account.helper;

import com.binance.account.api.UserApi;
import com.binance.account.vo.user.ex.UserStatusEx;
import com.binance.account.vo.user.response.GetUserResponse;
import com.binance.accountsubuser.api.FlexLineSubApi;
import com.binance.accountsubuser.api.SubUserIsolatedMarginApi;
import com.binance.accountsubuser.vo.enums.SubAccountSummaryQueryType;
import com.binance.accountsubuser.vo.margin.request.QueryIsolatedMarginEquityRequest;
import com.binance.accountsubuser.vo.margin.request.QuerySubAccountIsolatedMarginPageRequest;
import com.binance.accountsubuser.vo.margin.response.QuerySubAccountIsolatedMarginSummaryResp;
import com.binance.accountsubuser.vo.subuser.request.FlexLineQuerySubUserReq;
import com.binance.accountsubuser.vo.subuser.response.FlexLineQuerySubUserResp;
import com.binance.margin.isolated.api.profit.ProfitApi;
import com.binance.margin.isolated.api.profit.request.PeriodType;
import com.binance.margin.isolated.api.profit.response.ProfitSummaryResponse;
import com.binance.margin.isolated.api.user.UserBridgeApi;
import com.binance.margin.isolated.api.user.response.AccountDetailsResponse;
import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.account.account.vo.subuser.IsolatedMarginAccountDetailsRet;
import com.binance.mgs.account.account.vo.subuser.ProfitSummaryRet;
import com.binance.mgs.account.account.vo.subuser.QueryIsolatedMarginAccountSummaryArg;
import com.binance.mgs.account.account.vo.subuser.QuerySubUserIsolatedMarginDetailArg;
import com.binance.mgs.account.account.vo.subuser.QuerySubUserIsolatedMarginSummaryRet;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonPageRet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class IsolatedMarginHelper extends BaseHelper {

    private static final String NO_OPEN_MARGIN_ACCOUNT = "051002";

    private static final String NO_OPEN_ISOLATED_MARGIN_ACCOUNT = "128003";

    private static final Map<String, ProfitSummaryRet.ProfitDetail> EMPTY_PROFITS;

    private static final List<String> MARKETS = Lists.newArrayList("BTC", "USDT");

    private static final ProfitSummaryRet.ProfitDetail EMPTY_PROFIT = ProfitSummaryRet.ProfitDetail.builder()
            .profit(BigDecimal.ZERO)
            .profitRate(BigDecimal.ZERO)
            .build();

    static {
        EMPTY_PROFITS = MARKETS.stream().collect(Collectors.toMap(Function.identity(), market -> EMPTY_PROFIT));
    }

    private static final Set<String> NO_OPEN_ACCOUNT_CODES = Sets.newHashSet(
            NO_OPEN_MARGIN_ACCOUNT, NO_OPEN_ISOLATED_MARGIN_ACCOUNT);

    @Value("${sub.user.get.all.flexline.parent:true}")
    private Boolean querySubUserToGetAllFlexLineParentId;

    @Autowired
    private FlexLineSubApi flexLineSubApi;

    @Autowired
    private UserBridgeApi userBridgeApi;

    @Autowired
    private ProfitApi profitApi;

    @Autowired
    private UserApi userApi;

    @Autowired
    private SubUserIsolatedMarginApi subUserIsolatedMarginApi;

    public IsolatedMarginAccountDetailsRet querySubAccountIsolatedMarginDetail(QuerySubUserIsolatedMarginDetailArg arg, Long subUserId) {
        APIResponse<AccountDetailsResponse> response = userBridgeApi.accountDetails(subUserId, arg.getSymbols(), arg.isOnlyCreated());
        checkResponse(response);

        AccountDetailsResponse accountDetails = response.getData();
        IsolatedMarginAccountDetailsRet isolatedMarginAccountDetailsRet = IsolatedMarginAccountDetailsRet
                .builder()
                .totalAssetOfBtc(accountDetails.getTotalAssetOfBtc())
                .totalLiabilityOfBtc(accountDetails.getTotalLiabilityOfBtc())
                .totalNetAssetOfBtc(accountDetails.getTotalNetAssetOfBtc())
                .build();

        // BeanUtils会范型擦除
        isolatedMarginAccountDetailsRet.transferDetails(accountDetails.getDetails());
        return isolatedMarginAccountDetailsRet;
    }

    public ProfitSummaryRet queryIsolatedMarginProfit(Long subUserId, String symbol, PeriodType periodType) {
        APIResponse<ProfitSummaryResponse> response = profitApi.profit(subUserId, symbol, periodType);
        checkResponse(response);
        if (isNotOpenAccount(response)) {
            return buildEmptyProfit(System.currentTimeMillis());
        }
        ProfitSummaryResponse profitSummary = response.getData();
        ProfitSummaryRet profitSummaryRet = ProfitSummaryRet
                .builder()
                .beginTime(profitSummary.getBeginTime())
                .calcTime(profitSummary.getCalcTime())
                .build();

        profitSummaryRet.transferProfits(profitSummary.getProfits());
        return profitSummaryRet;
    }

    public CommonPageRet<QuerySubUserIsolatedMarginSummaryRet> querySubAccountTotalPageSummary(QueryIsolatedMarginAccountSummaryArg request) throws Exception {
        CommonPageRet<QuerySubUserIsolatedMarginSummaryRet> resp = new CommonPageRet<>();

        QuerySubAccountIsolatedMarginPageRequest req = new QuerySubAccountIsolatedMarginPageRequest();
        req.setEmail(req.getEmail());
        req.setParentUserId(getUserId());

        if (StringUtils.isNotBlank(request.getIsSubUserEnabled())) {
            req.setIsSubUserEnabled(Integer.valueOf(request.getIsSubUserEnabled()));
        }
        if (StringUtils.isNotBlank(request.getSubUserEmail())) {
            req.setEmail(request.getSubUserEmail());
        }

        req.setPage(request.getPage());
        req.setRows(request.getRows());
        APIResponse<QuerySubAccountIsolatedMarginSummaryResp> response = subUserIsolatedMarginApi.querySubAccountTotalPageSummary(APIRequest.instance(req));
        checkResponse(response);
        QuerySubAccountIsolatedMarginSummaryResp summaryResp = response.getData();
        if (summaryResp == null) {
            resp.setTotal(0);
            resp.setData(Lists.newArrayList());
            return resp;
        }
        resp.setTotal(summaryResp.getTotalSubAccountSummarySize());

        FlexLineQuerySubUserResp flexLineSubData = new FlexLineQuerySubUserResp();
        Map<String,Long> emailAndUserIdMap = new HashMap<>();
        Map<Long,String> userIdAndSubTypeMap = new HashMap<>();
        if(querySubUserToGetAllFlexLineParentId){
            APIResponse<List<Long>> allAvailableFlexLineParent = flexLineSubApi.getAllAvailableFlexLineParent();
            checkResponse(allAvailableFlexLineParent);
            List<Long> flexLineParentUserIds = allAvailableFlexLineParent.getData();
            if(!org.springframework.util.CollectionUtils.isEmpty(flexLineParentUserIds) && flexLineParentUserIds.contains(req.getParentUserId())){
                FlexLineQuerySubUserReq flexLineQuerySubUserReq = new FlexLineQuerySubUserReq();
                flexLineQuerySubUserReq.setParentUserId(req.getParentUserId());
                APIResponse<FlexLineQuerySubUserResp> queryFlexLineSub = flexLineSubApi.queryFlexLineSub(APIRequest.instance(flexLineQuerySubUserReq));
                checkResponse(queryFlexLineSub);
                flexLineSubData = queryFlexLineSub.getData();
            }
            if(flexLineSubData.getCreditSubUserId()!=null){
                GetUserResponse creditSubUserResp = getUserById(flexLineSubData.getCreditSubUserId());
                emailAndUserIdMap.put(creditSubUserResp.getUser().getEmail(),creditSubUserResp.getUser().getUserId());
                userIdAndSubTypeMap.put(creditSubUserResp.getUser().getUserId(),"creditSub");
            }
            if (CollectionUtils.isNotEmpty(flexLineSubData.getTradingSubUserIds())){
                for (Long tradingSubUserId : flexLineSubData.getTradingSubUserIds()) {
                    GetUserResponse tradingSubUserResp = getUserById(tradingSubUserId);
                    emailAndUserIdMap.put(tradingSubUserResp.getUser().getEmail(),tradingSubUserResp.getUser().getUserId());
                    userIdAndSubTypeMap.put(tradingSubUserResp.getUser().getUserId(),"tradingSub");
                }
            }
        }

        List<QuerySubUserIsolatedMarginSummaryRet> summaries = Lists.newArrayList();
        for (QuerySubAccountIsolatedMarginSummaryResp.UserTotalAssetSummary isolatedMarginSummary : summaryResp.getIsolatedMarginSummaries()) {
            QuerySubUserIsolatedMarginSummaryRet subUserIsolatedMarginSummaryRet = new QuerySubUserIsolatedMarginSummaryRet();
            subUserIsolatedMarginSummaryRet.setEmail(isolatedMarginSummary.getEmail());
            subUserIsolatedMarginSummaryRet.setTotalAssetOfBtc(isolatedMarginSummary.getTotalAssetOfBtc().toPlainString());
            subUserIsolatedMarginSummaryRet.setTotalLiabilityOfBtc(isolatedMarginSummary.getTotalLiabilityOfBtc().toPlainString());
            subUserIsolatedMarginSummaryRet.setTotalNetAssetOfBtc(isolatedMarginSummary.getTotalNetAssetOfBtc().toPlainString());
            subUserIsolatedMarginSummaryRet.setIsSubUserEnabled(new UserStatusEx(isolatedMarginSummary.getStatus()).getIsSubUserEnabled());
            Long flexLineSub = emailAndUserIdMap.get(isolatedMarginSummary.getEmail());
            if(flexLineSub != null){
                String subType = userIdAndSubTypeMap.get(flexLineSub);
                if(StringUtils.equals("creditSub",subType)){
                    subUserIsolatedMarginSummaryRet.setIsFlexLineCreditUser(true);
                }
                if (StringUtils.equals("tradingSub",subType)){
                    subUserIsolatedMarginSummaryRet.setIsFlexLineTradingUser(true);
                }
            }
            summaries.add(subUserIsolatedMarginSummaryRet);
        }
        resp.setData(summaries);
        return resp;
    }

    public String queryIsolatedMarginAccountTotalEquity(Long parentUserId, SubAccountSummaryQueryType summaryQueryType) throws Exception {

        QueryIsolatedMarginEquityRequest request = new QueryIsolatedMarginEquityRequest();
        request.setParentUserId(parentUserId);
        request.setSummaryQueryType(summaryQueryType);

        APIResponse<BigDecimal> resp = subUserIsolatedMarginApi.queryAccountTotalAssetOfBtc(APIRequest.instance(request));
        checkResponse(resp);
        BigDecimal totalAssetOfBtc = resp.getData();
        if (totalAssetOfBtc.compareTo(BigDecimal.ZERO) < 0) {
            return "--";
        }
        return totalAssetOfBtc.toPlainString();
    }

    public GetUserResponse getUserById(Long userId)throws Exception{
        com.binance.account.vo.security.request.UserIdRequest request = new com.binance.account.vo.security.request.UserIdRequest();
        request.setUserId(userId);
        APIResponse<GetUserResponse> apiResponse = userApi.getUserById(APIRequest.instance(request));
        if (APIResponse.Status.ERROR == apiResponse.getStatus()|| null==apiResponse.getData()) {
            log.error("SubUserSpotController UserApiClient.getUserById :userId=" + userId + "  error" + apiResponse.getErrorData());
            throw new BusinessException("getUserById failed");
        }
        return apiResponse.getData();
    }

    private boolean isNotOpenAccount(APIResponse<?> response) {
        return Objects.nonNull(response)
                && !APIResponse.OK.getStatus().equals(response.getStatus())
                && NO_OPEN_ACCOUNT_CODES.contains(response.getCode());
    }

    private ProfitSummaryRet buildEmptyProfit(Long time) {
        return ProfitSummaryRet.builder()
                .beginTime(time)
                .calcTime(time)
                .profits(EMPTY_PROFITS)
                .build();
    }
}
