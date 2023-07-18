package com.binance.mgs.account.account.controller;

import com.binance.account.api.SubUserApi;
import com.binance.account.api.UserApi;
import com.binance.account.vo.security.request.GetUserStatusByUserIdRequest;
import com.binance.account.vo.security.request.UserIdRequest;
import com.binance.account.vo.subuser.SubUserEmailVo;
import com.binance.account.vo.subuser.request.UserIdReq;
import com.binance.account.vo.subuser.response.SubUserTypeResponse;
import com.binance.account.vo.user.ex.UserStatusEx;
import com.binance.account.vo.user.response.GetUserResponse;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.StringUtils;
import com.binance.mgs.account.account.controller.client.SubUserClient;
import com.binance.mgs.account.account.vo.OpenOrderRet;
import com.binance.mgs.account.account.vo.OrderRet;
import com.binance.mgs.account.account.vo.TradeRet;
import com.binance.mgs.account.account.vo.subuser.SubAccQueryOpenOrderArg;
import com.binance.mgs.account.account.vo.subuser.SubAccQueryUserOrdersArg;
import com.binance.mgs.account.account.vo.subuser.SubQueryUserTradeArg;
import com.binance.mgs.account.service.VerifyRelationService;
import com.binance.platform.mgs.base.BaseAction;
import com.binance.platform.mgs.base.vo.CommonPageRet;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.business.account.helper.SubAccountHelper;
import com.binance.streamer.api.order.OrderApi;
import com.binance.streamer.api.request.order.QuerySubAccountOpenOrderRequest;
import com.binance.streamer.api.request.trade.QueryUserOrdersRequest;
import com.binance.streamer.api.request.trade.QueryUserTradeRequest;
import com.binance.streamer.api.response.SearchResult;
import com.binance.streamer.api.response.vo.OpenOrderVo;
import com.binance.streamer.api.response.vo.OrderVo;
import com.binance.streamer.api.response.vo.TradeVo;
import com.binance.streamer.api.trade.TradeApi;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/private/account/subUser/spot")
public class SubUserSpotController extends BaseAction {

    @Autowired
    private OrderApi orderApi;
    @Autowired
    private SubUserApi subUserApi;
    @Autowired
    private SubUserClient subUserClient;
    @Autowired
    private TradeApi tradeApi;
    @Autowired
    private UserApi userApi;
    @Autowired
    private VerifyRelationService verifyRelationService;
    @Value("${sub.user.spot.order.check.relation.switch:false}")
    private boolean checkRelationSwitch;


    /**
     * 子账号用户历史委托查询
     *
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/subaccount/get-trade-orders")
    @ApiOperation(value = "子账号用户历史委托查询")
    public CommonPageRet<OrderRet> getSubTradeOrders(@Valid @RequestBody SubAccQueryUserOrdersArg queryUserOrdersArg) throws Exception {
        Pair<Long, Boolean> pair = getRealParentUserId();
        final Long parentUserId = pair.getLeft();
        log.info("SubUserSpotController queryUserOrdersArg={}", JsonUtils.toJsonNotNullKey(queryUserOrdersArg));
        UserIdReq parentUserIdReq = new UserIdReq();
        parentUserIdReq.setUserId(parentUserId);
        APIResponse<SubUserTypeResponse> accApiResponse = subUserClient.checkRelationByUserId(getInstance(parentUserIdReq));
        checkResponse(accApiResponse);
        SubUserTypeResponse subUserTypeResponse = accApiResponse.getData();
        List<Long> subUserIds = subUserTypeResponse.getSubUserIds();
        List<SubUserEmailVo> subUserEmailVos = subUserTypeResponse.getSubUserIdEmails();
        final String subUserId = queryUserOrdersArg.getUserId();
        boolean commMerchantSubUserFlag = checkIsCommonMerchantSubUser(subUserId);

        // 当前子账号不是commonMerchantSubUser时，才做如下校验
        if(!commMerchantSubUserFlag){
            // 确保是母账户 && 确保入参为当前登陆账户 或 当前登陆账户的子账户
            if (checkRelationSwitch) {
                verifyRelationService.checkParentAndSubRelation(parentUserId, subUserTypeResponse.getUserType(), subUserId);
            } else {
                SubAccountHelper.assertTrueWithInputUserId(parentUserId, subUserTypeResponse.getUserType(), subUserIds, subUserId);
            }
        }

        CommonPageRet<OrderRet> response = new CommonPageRet<>();

        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(subUserIds) && org.apache.commons.collections4.CollectionUtils.isNotEmpty(subUserEmailVos)) {
            com.binance.streamer.api.request.trade.QueryUserOrdersRequest queryUserOrdersRequest = new QueryUserOrdersRequest();
            BeanUtils.copyProperties(queryUserOrdersArg, queryUserOrdersRequest);

            if (StringUtils.isNotBlank(subUserId) && !commMerchantSubUserFlag ) {
                subUserIds.clear();
                subUserIds.add(Long.valueOf(subUserId));
                queryUserOrdersRequest.setUserIds(subUserIds);
            } else {
                queryUserOrdersRequest.setUserIds(subUserIds);
                queryUserOrdersRequest.getUserIds().add(parentUserId);
            }

            APIResponse<SearchResult<OrderVo>> streamerApiResponse = orderApi.queryUserOrders(getInstance(queryUserOrdersRequest));
            checkResponse(streamerApiResponse);

            SearchResult<com.binance.streamer.api.response.vo.OrderVo> result = streamerApiResponse.getData();

            if (result != null && !org.apache.commons.collections4.CollectionUtils.isEmpty(result.getRows())) {
                List<OrderRet> data = new ArrayList<>();
                for (OrderVo orderVo : result.getRows()) {
                    for (SubUserEmailVo subUserEmailVo : subUserEmailVos) {
                        if (parentUserId.compareTo(orderVo.getUserId()) == 0) {
                            OrderRet orderRet = new OrderRet();
                            BeanUtils.copyProperties(orderVo, orderRet);
                            orderRet.setEmail(getRealUserEmail(pair));
                            data.add(orderRet);
                            break;
                        }
                        if (subUserEmailVo.getUserId().compareTo(orderVo.getUserId()) == 0) {
                            OrderRet orderRet = new OrderRet();
                            BeanUtils.copyProperties(orderVo, orderRet);
                            orderRet.setEmail(subUserEmailVo.getEmail());
                            data.add(orderRet);
                            break;
                        }
                    }
                }
                response.setData(data);
                response.setTotal(result.getTotal());
            }
        }
        return response;
    }

    private String getRealUserEmail(Pair<Long, Boolean> pair) throws Exception {
        if(pair.getRight()){
            return this.getUserById(pair.getLeft()).getUser().getEmail();
        }else{
            return getUserEmail();
        }
    }


    /**
     * 查询用户所有子账户未完成的订单列表
     *
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/subaccount/get-open-orders")
    @ApiOperation(value = "查询用户所有子账户未完成的订单列表")
    public CommonRet<List<OpenOrderRet>> getSubOpenOrders(@Valid @RequestBody SubAccQueryOpenOrderArg queryOpenOrderArg) throws Exception {

        Pair<Long, Boolean> pair = getRealParentUserId();
        final Long parentUserId = pair.getLeft();
        log.info("SubUserSpotController queryOpenOrderArg={}", JsonUtils.toJsonNotNullKey(queryOpenOrderArg));
        UserIdReq parentUserIdReq = new UserIdReq();
        parentUserIdReq.setUserId(parentUserId);
        APIResponse<SubUserTypeResponse> accApiResponse = subUserClient.checkRelationByUserId(getInstance(parentUserIdReq));

        checkResponse(accApiResponse);
        SubUserTypeResponse subUserTypeResponse = accApiResponse.getData();
        List<Long> subUserIds = subUserTypeResponse.getSubUserIds();
        List<SubUserEmailVo> subUserEmailVos = subUserTypeResponse.getSubUserIdEmails();
        final String subUserId = queryOpenOrderArg.getUserId();

        boolean commMerchantSubUserFlag = checkIsCommonMerchantSubUser(subUserId);

        // 当前子账号不是commonMerchantSubUser时，才做如下校验
        if(!commMerchantSubUserFlag){
            // 确保是母账户 && 确保入参为当前登陆账户 或 当前登陆账户的子账户
            if (checkRelationSwitch) {
                verifyRelationService.checkParentAndSubRelation(parentUserId, subUserTypeResponse.getUserType(), subUserId);
            } else {
                SubAccountHelper.assertTrueWithInputUserId(parentUserId, subUserTypeResponse.getUserType(), subUserIds, subUserId);
            }
        }
        CommonRet<List<OpenOrderRet>> response = new CommonRet<>();

        if (CollectionUtils.isNotEmpty(subUserIds) && CollectionUtils.isNotEmpty(subUserEmailVos)) {
            QuerySubAccountOpenOrderRequest queryOpenOrderRequest = new QuerySubAccountOpenOrderRequest();
            BeanUtils.copyProperties(queryOpenOrderArg, queryOpenOrderRequest);

            if (StringUtils.isNotBlank(subUserId) && !commMerchantSubUserFlag) {
                subUserIds.clear();
                subUserIds.add(Long.valueOf(subUserId));
                queryOpenOrderRequest.setUserIds(subUserIds);
            } else {
                queryOpenOrderRequest.setUserIds(subUserIds);
                queryOpenOrderRequest.getUserIds().add(parentUserId);
            }
            APIResponse<SearchResult<OpenOrderVo>> streamerApiResponse = orderApi.querySubAccountOpenOrder(getInstance(queryOpenOrderRequest));
            checkResponse(streamerApiResponse);

            SearchResult<OpenOrderVo> result = streamerApiResponse.getData();
            if (result != null && !CollectionUtils.isEmpty(result.getRows())) {
                List<OpenOrderRet> data = new ArrayList<>();
                for (OpenOrderVo openOrderVo : result.getRows()) {
                    for (SubUserEmailVo subUserEmailVo : subUserEmailVos) {
                        if (parentUserId.compareTo(Long.valueOf(openOrderVo.getUserId())) == 0) {
                            OpenOrderRet openOrderRet = new OpenOrderRet();
                            BeanUtils.copyProperties(openOrderVo, openOrderRet);
                            openOrderRet.setEmail(getRealUserEmail(pair));
                            data.add(openOrderRet);
                            break;
                        }
                        if (subUserEmailVo.getUserId().compareTo(Long.valueOf(openOrderVo.getUserId())) == 0) {
                            OpenOrderRet openOrderRet = new OpenOrderRet();
                            BeanUtils.copyProperties(openOrderVo, openOrderRet);
                            openOrderRet.setEmail(subUserEmailVo.getEmail());
                            data.add(openOrderRet);
                            break;
                        }
                    }

                }
                response.setData(data);
            }
        }
        return response;
    }

    /**
     * 查询子账户用户历史成交
     *
     * @return
     */
    @ApiOperation(value = "查询子账户用户历史成交")
    @PostMapping(value = "subaccount/get-user-trades")
    public CommonPageRet<TradeRet> getSubUserTrades(@Valid @RequestBody SubQueryUserTradeArg queryUserTradeArg)
            throws Exception {

        Pair<Long, Boolean> pair = getRealParentUserId();
        final Long parentUserId = pair.getLeft();

        log.info("SubUserSpotController queryUserTradeArg={}", JsonUtils.toJsonNotNullKey(queryUserTradeArg));

        UserIdReq parentUserIdReq = new UserIdReq();
        parentUserIdReq.setUserId(parentUserId);
        log.info("parentUserIdReq={}", JsonUtils.toJsonNotNullKey(parentUserIdReq));
        APIResponse<SubUserTypeResponse> accApiResponse = subUserClient.checkRelationByUserId(getInstance(parentUserIdReq));
        checkResponse(accApiResponse);
        SubUserTypeResponse subUserTypeResponse = accApiResponse.getData();
        List<Long> subUserIds = subUserTypeResponse.getSubUserIds();
        List<SubUserEmailVo> subUserEmailVos = subUserTypeResponse.getSubUserIdEmails();
        final String subUserId = queryUserTradeArg.getUserId();

        boolean commMerchantSubUserFlag = checkIsCommonMerchantSubUser(subUserId);

        // 当前子账号不是commonMerchantSubUser时，才做如下校验
        if(!commMerchantSubUserFlag){
            // 确保是母账户 && 确保入参为当前登陆账户 或 当前登陆账户的子账户
            if (checkRelationSwitch) {
                verifyRelationService.checkParentAndSubRelation(parentUserId, subUserTypeResponse.getUserType(), subUserId);
            } else {
                SubAccountHelper.assertTrueWithInputUserId(parentUserId, subUserTypeResponse.getUserType(), subUserIds, subUserId);
            }
        }
        CommonPageRet<TradeRet> response = new CommonPageRet<>();

        if (CollectionUtils.isNotEmpty(subUserIds) && CollectionUtils.isNotEmpty(subUserEmailVos)) {
            QueryUserTradeRequest queryUserTradeRequest = new QueryUserTradeRequest();
            BeanUtils.copyProperties(queryUserTradeArg, queryUserTradeRequest);

            if (StringUtils.isNotBlank(subUserId) && !commMerchantSubUserFlag) {
                subUserIds.clear();
                subUserIds.add(Long.valueOf(subUserId));
                queryUserTradeRequest.setUserIds(subUserIds);
            } else {
                queryUserTradeRequest.setUserIds(subUserIds);
                queryUserTradeRequest.getUserIds().add(parentUserId);
            }
            queryUserTradeRequest.setUserIds(subUserIds);
            log.info("queryUserTradeRequest={}", JsonUtils.toJsonNotNullKey(queryUserTradeRequest));
            APIResponse<SearchResult<TradeVo>> apiResponse = tradeApi.getUserTrades(getInstance(queryUserTradeRequest));
            checkResponse(apiResponse);
            SearchResult<TradeVo> result = apiResponse.getData();

            if (result != null && !CollectionUtils.isEmpty(result.getRows())) {
                List<TradeRet> data = new ArrayList<>();
                for (TradeVo tradeVo : result.getRows()) {
                    for (SubUserEmailVo subUserEmailVo : subUserEmailVos) {
                        if (parentUserId.compareTo(tradeVo.getUserId()) == 0) {
                            TradeRet tradeRet = new TradeRet();
                            BeanUtils.copyProperties(tradeVo, tradeRet);
                            tradeRet.setEmail(getRealUserEmail(pair));
                            data.add(tradeRet);
                            break;
                        }
                        if (subUserEmailVo.getUserId().compareTo(tradeVo.getUserId()) == 0) {
                            TradeRet tradeRet = new TradeRet();
                            BeanUtils.copyProperties(tradeVo, tradeRet);
                            tradeRet.setEmail(subUserEmailVo.getEmail());
                            data.add(tradeRet);
                            break;
                        }
                    }
                }
                response.setData(data);
                response.setTotal(result.getTotal());
            }
        }
        return response;
    }

    private boolean checkIsCommonMerchantSubUser(String subUserId) {
        if(org.apache.commons.lang3.StringUtils.isBlank(subUserId)){
            return false;
        }
        GetUserStatusByUserIdRequest userIdRequest = new GetUserStatusByUserIdRequest();
        userIdRequest.setUserId(Long.valueOf(subUserId));
        try {
            APIResponse<UserStatusEx> apiResponse = userApi.getUserStatusByUserIdFromReadOrWriteDb(getInstance(userIdRequest));
            if (!baseHelper.isOk(apiResponse)) {
                log.info("checkIsCommonMerchantSubUser error, response = {} ", apiResponse);
                throw new BusinessException(apiResponse.getCode(), baseHelper.getErrorMsg(apiResponse), apiResponse.getParams());
            }
            return apiResponse.getData().getIsCommonMerchantSubUser();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e1){
            log.error("userApi.getUserStatusByUserIdFromReadOrWriteDb error",e1);
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }
    }


    /**
     *  当前登录账号如果isCommonMerchantSubUser = true， 则将当前账户替换为其母账户，再做相关查询
     */
    private Pair<Long, Boolean>  getRealParentUserId() throws Exception {
        GetUserResponse userResponse = this.getUserById(getUserId());
        if(userResponse != null && userResponse.getUser() != null ){
            UserStatusEx userStatusEx = new UserStatusEx(userResponse.getUser().getStatus(),userResponse.getUser().getStatusExtra());
            if(userStatusEx.getIsCommonMerchantSubUser()){
                return Pair.of(userResponse.getUserInfo().getParent(),true);
            }
        }
        return Pair.of(getUserId(),false);
    }

    public GetUserResponse getUserById(Long userId)throws Exception{
        UserIdRequest request = new UserIdRequest();
        request.setUserId(userId);
        APIResponse<GetUserResponse> apiResponse = userApi.getUserById(APIRequest.instance(request));
        if (APIResponse.Status.ERROR == apiResponse.getStatus()|| null==apiResponse.getData()) {
            log.error("SubUserSpotController UserApiClient.getUserById :userId=" + userId + "  error" + apiResponse.getErrorData());
            throw new BusinessException("getUserById failed");
        }
        return apiResponse.getData();
    }


}
