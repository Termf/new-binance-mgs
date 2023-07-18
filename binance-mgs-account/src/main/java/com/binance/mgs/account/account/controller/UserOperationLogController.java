package com.binance.mgs.account.account.controller;

import com.binance.account.api.UserOperationLogApi;
import com.binance.account.vo.operationlog.UserOperationLogVo;
import com.binance.account.vo.security.request.UserOperationLogUserViewRequest;
import com.binance.account.vo.security.response.UserOperationLogResultResponse;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.mgs.account.account.vo.QueryUserOperationLogArg;
import com.binance.mgs.account.advice.AccountDefenseResource;
import com.binance.platform.mgs.base.BaseAction;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.userbigdata.api.UserOperationLogESApi;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(value = "/v1")
public class UserOperationLogController extends BaseAction {

    @Autowired
    private UserOperationLogApi userOperationLogApi;
    @Autowired
    private UserOperationLogESApi userOperationLogESApi;


    @Value("#{'${user.visible.operations:}'.split(',')}")
    private List<String> userVisibleOperations;
    @Value("${user.operation.log.query.switch:false}")
    private Boolean userOperationLogQuerySwitch;
    @Value("${user.operation.log.time.valid.switch:false}")
    private Boolean userOperationLogTimeValidSwitch;

    private APIResponse<UserOperationLogResultResponse> listUserOperationLogsUserView(UserOperationLogUserViewRequest request) throws Exception {
        if (userOperationLogQuerySwitch) {
            com.binance.userbigdata.vo.security.request.UserOperationLogUserViewRequest viewRequest = CopyBeanUtils.copy(request, com.binance.userbigdata.vo.security.request.UserOperationLogUserViewRequest.class);
            APIResponse<com.binance.userbigdata.vo.security.response.UserOperationLogResultResponse> apiResponse = userOperationLogESApi.listUserOperationLogsUserView(getInstance(viewRequest));
            checkResponse(apiResponse);

            UserOperationLogResultResponse response = CopyBeanUtils.copy(apiResponse.getData(), UserOperationLogResultResponse.class);
            return APIResponse.getOKJsonResult(response);
        } else {
            return userOperationLogApi.listUserOperationLogsUserView(getInstance(request));
        }
    }

    @ApiOperation(value = "查询用户登陆日志")
    @PostMapping(value = "/private/account/user-login-log/query")
    @AccountDefenseResource(name="UserOperationLogController.queryUserLoginLog")
    public CommonRet<UserOperationLogResultResponse> queryUserLoginLog(
            @RequestBody @Validated QueryUserOperationLogArg queryUserOperationLogArg) throws Exception {
        Date startTime = queryUserOperationLogArg.getStartTime();
        Date endTime = queryUserOperationLogArg.getEndTime();
        if (userOperationLogTimeValidSwitch && (startTime == null || endTime == null || (endTime.getTime()- startTime.getTime() > 24*3600000))){
            if (startTime == null && endTime == null){
                startTime = getCurrentDay();
                endTime = new Date();
            }else if (startTime == null){
                startTime = getDayMin(endTime,-1);
            }else if (endTime == null){
                endTime = getDayMin(startTime,1);
            }else{
                if (endTime.getTime() - startTime.getTime() >  3600000*24){
                    endTime = getDayMin(startTime,1);
                }
            }
        }
        queryUserOperationLogArg.setStartTime(startTime);
        queryUserOperationLogArg.setEndTime(endTime);
        queryUserOperationLogArg.setOperations(Collections.singletonList("用户登陆"));
        return queryUserOperationLog(queryUserOperationLogArg);
    }

    /**
     * 取得当前天后，加上指定天数后的最小时间
     *
     * @param date 当前日期
     * @param addDay 天数
     * @return 当前天后，加上指定天数后的最小时间
     */
    public static Date getDayMin(Date date, int addDay) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, addDay);

        return cal.getTime();
    }

    public static Date getCurrentDay() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }


    @ApiOperation(value = "查询用户登陆日志")
    @PostMapping(value = "/private/account/user-operation-log/query")
    @AccountDefenseResource(name="UserOperationLogController.queryUserOperationLog1")
    public CommonRet<UserOperationLogResultResponse> queryUserOperationLog1(
            @RequestBody @Validated QueryUserOperationLogArg queryUserOperationLogArg) throws Exception {
        Date endTime = queryUserOperationLogArg.getEndTime();
        Date startTime = queryUserOperationLogArg.getStartTime();
        if (CollectionUtils.isEmpty(queryUserOperationLogArg.getOperations())) {
            queryUserOperationLogArg.setOperations(userVisibleOperations);
        }
        if (userOperationLogTimeValidSwitch && (startTime == null || endTime == null || (endTime.getTime()- startTime.getTime() > 3600000*24))){
            if (startTime == null && endTime == null){
                startTime = getCurrentDay();
                endTime = new Date();
            }else if (startTime == null){
                startTime = getDayMin(endTime,-1);
            }else if (endTime == null){
                endTime = getDayMin(startTime,1);
            }else{
                if (endTime.getTime() - startTime.getTime() >  3600000*24){
                    endTime = getDayMin(startTime,1);
                }
            }
        }
        queryUserOperationLogArg.setStartTime(startTime);
        queryUserOperationLogArg.setEndTime(endTime);
        return queryUserOperationLog(queryUserOperationLogArg);
    }

    private CommonRet<UserOperationLogResultResponse> queryUserOperationLog(
            @RequestBody @Validated QueryUserOperationLogArg queryUserOperationLogArg) throws Exception {
        CommonRet<UserOperationLogResultResponse> response = new CommonRet<>();
        UserOperationLogUserViewRequest uolRequest = new UserOperationLogUserViewRequest();
        handleQueryArg(queryUserOperationLogArg);
        BeanUtils.copyProperties(queryUserOperationLogArg, uolRequest);
        uolRequest.setOffset((queryUserOperationLogArg.getPage() - 1) * queryUserOperationLogArg.getRows());
        uolRequest.setLimit(queryUserOperationLogArg.getRows());
        uolRequest.setRequestTimeFrom(queryUserOperationLogArg.getStartTime());
        uolRequest.setRequestTimeTo(queryUserOperationLogArg.getEndTime());
        uolRequest.setUserId(getUserId());
        if (!CollectionUtils.isEmpty(queryUserOperationLogArg.getOperations())) {
            uolRequest.setOperations(new ArrayList<>(queryUserOperationLogArg.getOperations()));
        }
        APIResponse<UserOperationLogResultResponse> apiResponse = listUserOperationLogsUserView(uolRequest);
        checkResponse(apiResponse);
        UserOperationLogResultResponse resultResponse = apiResponse.getData();
        if (null != resultResponse && resultResponse.getRows() != null) {
            for (UserOperationLogVo logVo : resultResponse.getRows()) {
                logVo.setRequest(null);
                logVo.setResponse(null);
                logVo.setEmail(null);
                logVo.setFullIp(null);
                logVo.setId(null);
                logVo.setEmail(null);
                logVo.setApikey(null);
            }
            response.setData(resultResponse);
        }
        return response;
    }


    private void handleQueryArg(QueryUserOperationLogArg queryUserOperationLogArg) {
        //最多查询一年
        final long maxDuration = TimeUnit.DAYS.toMillis(365);
        if (queryUserOperationLogArg.getStartTime() != null && queryUserOperationLogArg.getEndTime() != null) {
            if (queryUserOperationLogArg.getEndTime().getTime() - queryUserOperationLogArg.getStartTime().getTime() > maxDuration
                || queryUserOperationLogArg.getEndTime().getTime() < queryUserOperationLogArg.getStartTime().getTime()) {
                //恶意请求
                throw new IllegalArgumentException();
            }
        } else if (queryUserOperationLogArg.getStartTime() == null && queryUserOperationLogArg.getEndTime() != null) {
            queryUserOperationLogArg.setStartTime(new Date(queryUserOperationLogArg.getEndTime().getTime() - maxDuration));
        } else if (queryUserOperationLogArg.getStartTime() != null && queryUserOperationLogArg.getEndTime() == null) {
            queryUserOperationLogArg.setEndTime(new Date(queryUserOperationLogArg.getStartTime().getTime() + maxDuration));
        } else {
            queryUserOperationLogArg.setEndTime(new Date());
            queryUserOperationLogArg.setStartTime(new Date(System.currentTimeMillis() - maxDuration));
        }
    }

}
