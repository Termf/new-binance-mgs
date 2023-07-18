package com.binance.mgs.account.marketing.controller;

import com.binance.account.api.UserApi;
import com.binance.account.vo.subuser.request.UserIdReq;
import com.binance.account.vo.user.response.FinanceFlagResponse;
import com.binance.marketing.api.UserOperationRecApi;
import com.binance.marketing.vo.userOperRec.request.SaveUserOperRecRequest;
import com.binance.marketing.vo.userOperRec.response.SaveUserOperRecResponse;
import com.binance.marketing.vo.userOperRec.response.UserOperRecDTO;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.StringUtils;
import com.binance.mgs.account.marketing.vo.GetUserFirstPageRet;
import com.binance.mgs.account.marketing.vo.UserOperArg;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.business.AbstractBaseAction;
import com.binance.userbigdata.api.BigDataUserApi;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.concurrent.TimeUnit;

/**
 * 接口文档： https://confluence.toolsfdg.net/pages/viewpage.action?pageId=45721942
 * 调用的marketing接口，但是配置类的要写在account里面，account不支持marketing serviceName
 *
 * 迁移到了composite中
 */
@RestController
@Slf4j
@Deprecated
public class UserGuidePageController extends AbstractBaseAction {

    @Value("${financeFlag.userbigdata.switch:0}")
    private int financeFlagQuerySwitch;

    @Autowired
    private UserOperationRecApi userOperationRecApi;

    @Autowired
    private UserApi userApi;
    @Autowired
    private BigDataUserApi bigDataUserApi;

    private final static String BIZ_TYPE = "USER_LAYER";
    private final static String OPER_RESULT = "y";

    private LoadingCache<Long, Integer> firstPageCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .refreshAfterWrite(90, TimeUnit.SECONDS)
            .build(new CacheLoader<Long, Integer>() {
                @Override
                public Integer load(Long userId) throws Exception {
                    log.info("get user:{} first page from cache", userId);
                    return getUserFirstPage(userId);
                }
            });

    @ApiOperation(value = "上报用户分层接口")
    @PostMapping(value = "/v1/private/account/experience/put-user-layer")
    public CommonRet<Void> putUserLayer(@Valid @RequestBody UserOperArg userOperArg) throws Exception {
        if(userOperArg == null || userOperArg.getActionType() == null){
            throw new BusinessException(GeneralCode.USER_ILLEGAL_PARAMETER);
        }
        Long userId = getUserId();
        this.putUserLayer(userId, userOperArg);
        CommonRet<Void> ret = new CommonRet<>();
        return ret;
    }

    @ApiOperation(value = "上报用户分层数据并查询用户引导页")
    @PostMapping(value = "/v1/private/account/experience/put-get-first-page")
    public CommonRet<GetUserFirstPageRet> putGetFirstPage(@Valid @RequestBody UserOperArg userOperArg) throws Exception {
        Long userId = getUserId();
        if(userOperArg != null && userOperArg.getActionType() == UserOperArg.ActionTypeEnum.MATURE ){
            this.putUserLayer(userId, userOperArg);
        }
        Integer guidePage = null;
        try{
            guidePage = firstPageCache.get(userId);
        }catch (Exception e){
            log.warn("put get user:{} first page exception:", userId, e);
            guidePage = getUserFirstPage(userId);
        }
        log.info("put get user:{} first page:{}", userId, guidePage);
        CommonRet<GetUserFirstPageRet> ret = new CommonRet<>();
        GetUserFirstPageRet getUserFirstPageRet = new GetUserFirstPageRet();
        getUserFirstPageRet.setGuidePage(guidePage);
        ret.setData(getUserFirstPageRet);
        return ret;
    }

    private UserOperRecDTO getUserOperationRec(Long userId, String actionType){
        APIResponse<SaveUserOperRecResponse> recResponseAPIResponse = userOperationRecApi.queryUserOperRec(userId, BIZ_TYPE, actionType);
        checkResponse(recResponseAPIResponse);
        UserOperRecDTO dto = null;
        if(recResponseAPIResponse.getData() != null &&  recResponseAPIResponse.getData().getUserOperRecs() != null){
            dto = recResponseAPIResponse.getData().getUserOperRecs().get(0);
        }
        return dto;
    }

    private UserOperRecDTO getNewUserOperationRec(Long userId){
        APIResponse<UserOperRecDTO> recResponseAPIResponse = userOperationRecApi.queryNewUserOperRec(userId, BIZ_TYPE);
        checkResponse(recResponseAPIResponse);
        UserOperRecDTO dto = recResponseAPIResponse.getData();
        return dto;
    }

    /**
     * 1-专业版首页
     * 2-新手版首页,引导认证
     * 3-新手版首页,引导快速买币
     * 4-新手版首页,引导现货下单
     */
    private Integer getUserFirstPage(Long userId) throws Exception {
        UserOperRecDTO dto = this.getNewUserOperationRec(userId);
        Integer guidePage = null;
        if(dto != null && StringUtils.equalsIgnoreCase(dto.getActionType(), UserOperArg.ActionTypeEnum.MATURE.name())){
            guidePage = 1;
        } else {
            FinanceFlagResponse financeFlagResponse = new FinanceFlagResponse();
            if (financeFlagQuerySwitch()) {
                log.info("getUserFirstPage invoke user-big-data");
                com.binance.userbigdata.vo.user.request.UserIdReq userIdReq = new com.binance.userbigdata.vo.user.request.UserIdReq();
                userIdReq.setUserId(userId);
                APIResponse<com.binance.userbigdata.vo.user.response.FinanceFlagResponse> responseAPIResponse = bigDataUserApi.financeFlag(getInstance(userIdReq));
                checkResponse(responseAPIResponse);
                BeanUtils.copyProperties(responseAPIResponse.getData(), financeFlagResponse);
            } else {
                log.info("getUserFirstPage invoke account");
                UserIdReq userIdReq = new UserIdReq();
                userIdReq.setUserId(userId);
                APIResponse<FinanceFlagResponse> responseAPIResponse = userApi.financeFlag(getInstance(userIdReq));
                checkResponse(responseAPIResponse);
                financeFlagResponse = responseAPIResponse.getData();
            }
            
            if(financeFlagResponse.getHasSpotOrder() || financeFlagResponse.getHasWithdrawRecord()){
                guidePage = 1;
            }else if(!financeFlagResponse.getIsKycPass()){
                guidePage = 2;
            }else if(!financeFlagResponse.getHasAsset()){
                guidePage = 3;
            }else{
                guidePage = 4;
            }
        }
        return guidePage;
    }
    
    private Boolean financeFlagQuerySwitch() {
        // 生成[1,10]的随机数
        int randomNum = RandomUtils.nextInt(10)+1;
        // 根据配置切流
        return financeFlagQuerySwitch >= randomNum;
    }

    private void putUserLayer(Long userId, UserOperArg userOperArg){
        log.info("putUserLayer userId={}, userOperArg={}", userId, userOperArg);
        String actionTypeStr = userOperArg.getActionType() + "";
        UserOperRecDTO dto = this.getUserOperationRec(userId, actionTypeStr);
        if(dto == null){
            SaveUserOperRecRequest request = new SaveUserOperRecRequest();
            request.setUserId(userId);
            request.setBizType(BIZ_TYPE);
            request.setActionType(actionTypeStr);
            request.setOperResult(OPER_RESULT);
            APIResponse<Void> response = userOperationRecApi.addUserOperRec(getInstance(request));
            checkResponse(response);
        }
    }
}
