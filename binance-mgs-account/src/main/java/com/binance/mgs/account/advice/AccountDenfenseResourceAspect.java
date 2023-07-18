package com.binance.mgs.account.advice;

import com.binance.accountdefensecenter.api.RuleApi;
import com.binance.accountdefensecenter.vo.rule.CheckRuleRequest;
import com.binance.accountdefensecenter.vo.rule.CheckRuleResponse;
import com.binance.accountdefensecenter.vo.rule.ErrorMessage;
import com.binance.master.constant.Constant;
import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.Interceptor.FeignAsyncHelper;
import com.binance.platform.common.RpcContext;
import com.binance.platform.common.TrackingUtils;
import com.binance.platform.env.EnvUtil;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.utils.DomainUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author pengchenxue
 * */
@Log4j2
@Component
@Aspect
@Order(OrderConfig.AccountDefenseResource_ORDER)
public class AccountDenfenseResourceAspect {
    @Resource
    private RuleApi ruleApi;
    @Resource
    private BaseHelper baseHelper;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${defense.global.switch:false}")
    private boolean defenseGlobalSwitch;


    @Value("${defense.timeout.check.switch:false}")
    private boolean defenseTimeoutSwitch;

    @Value("${defense.timeout:300}")
    private long defenseTimeOut;


    @Value("#{'${feign.async.request.interceptor.skipList:content-length}'.split(',')}")
    private List<String> requestHeadSkipList;

    @Value("#{'${defense.preserve.original.headers:user-agent}'.split(',')}")
    private List<String> originalHeaders;

    @Value("${defense.preserve.original.headers.prefix:original_}")
    private String originalHeadersPrefix;

    @Autowired
    @Qualifier("accountDefenseExecutor")
    private ExecutorService executorService;
    @Autowired
    @Qualifier("accountDefenseAfterServiceExecutor")
    private ExecutorService executorAfterService;



    @Pointcut("@annotation(com.binance.mgs.account.advice.AccountDefenseResource)")//指向自定义注解路径
    public void validatePointCut() {

    }


    /**
     * 环绕通知
     */
    @Around("@annotation(accountDefenseResource)")
    public Object accountDefenseResourceAround(ProceedingJoinPoint pjp, AccountDefenseResource accountDefenseResource) throws Throwable {
        Object obj = null;
        Throwable throwable = null;
        if(!defenseGlobalSwitch){
            obj=pjp.proceed();
            return obj;
        }
        if (null == accountDefenseResource||StringUtils.isBlank(accountDefenseResource.name())) {
            log.warn("AccountDenfenseResourceAspect.before：accountDefenseResource=null");
            obj=pjp.proceed();
            return obj;
        }

        Object[] args= pjp.getArgs();
        String domain = DomainUtils.getDomain();
        String ip = WebUtils.getRequestIp();
        String name=accountDefenseResource.name();
        Long userId=baseHelper.getUserId();
        CheckRuleRequest checkRuleRequest=new CheckRuleRequest();
        checkRuleRequest.setName(name);
        for (Object param : args) {
            try {
                //有些类型我们要过滤掉
                if (param instanceof HttpServletRequest || param instanceof HttpServletResponse || param instanceof String || param instanceof Integer
                        || param instanceof Long || param instanceof Boolean || param instanceof Double || param instanceof Float || param instanceof BigDecimal
                        || param instanceof Date || param instanceof MultipartFile) {
                    continue;
                }
                checkRuleRequest.setParam(new Object[]{param});
                break;
            } catch (Exception e) {
                log.error("failed to parse {}", param.getClass());
            }
        }
        checkRuleRequest.setDomain(domain);
        checkRuleRequest.setRequestIp(ip);
        checkRuleRequest.setUserId(userId);
        checkRuleRequest.setApplicationName(applicationName);
        checkRuleRequest.setParamHint(accountDefenseResource.paramHint());
        APIRequest<CheckRuleRequest> request= baseHelper.getInstance(checkRuleRequest);
        APIResponse<CheckRuleResponse> apiResponse=null;
        String envFlag = EnvUtil.getEnvFlag();
        String traceId = TrackingUtils.getTrace();
        HttpServletRequest httpServletRequest=WebUtils.getHttpServletRequest();
        Map<String, String> headMap=getHeadersInfo(httpServletRequest);
        if(defenseTimeoutSwitch){
            try{

                Future<APIResponse<CheckRuleResponse>> future = executorService.submit(new Callable<APIResponse<CheckRuleResponse>>() {
                    @Override
                    public APIResponse<CheckRuleResponse> call() throws Exception {
                        TrackingUtils.saveTrace(traceId);
                        RpcContext.getContext().set(Constant.GRAY_ENV_HEADER, envFlag);
                        FeignAsyncHelper.addHead(headMap);
                        try{
                            APIResponse<CheckRuleResponse> apiResponse=ruleApi.check(request);
                            return apiResponse;
                        }catch (Exception e){
                            log.warn("ruleApi.check,error", e);
                            throw e;
                        }
                    }
                });
                apiResponse= future.get(defenseTimeOut, TimeUnit.MILLISECONDS);
            }catch (TimeoutException e) {
                apiResponse=APIResponse.getOKJsonResult();
                log.warn("defenseTimeOut", e);
            } catch (Exception e) {
                apiResponse=APIResponse.getOKJsonResult();
                log.warn("defenseException", e);
            }
        }else{
            apiResponse =ruleApi.check(request);
        }
        if(!baseHelper.isOk(apiResponse)){
            baseHelper.checkResponse(apiResponse);
        }
        if (null!=apiResponse.getData() && null!=apiResponse.getData().getName()){
            throw new BusinessException(apiResponse.getData().getErrorCode(),apiResponse.getData().getErrorMessage());
        }

        try {
            //执行业务方法
            obj = pjp.proceed();
        } catch (Throwable e) {
            log.info("AccountDefenseResource proceed() error,", e);
            throwable = e;
            Throwable internalThrowable=e;
            try{
                if (null!=internalThrowable && internalThrowable instanceof BusinessException) {
                    BusinessException businessException = (BusinessException) internalThrowable;
                    ErrorMessage errorMessage=new ErrorMessage();
                    errorMessage.setErrorCode(null==businessException.getErrorCode()?businessException.getBizCode():businessException.getErrorCode().getCode());
                    errorMessage.setErrorMessage(null==businessException.getErrorCode()?businessException.getBizMessage():businessException.getErrorCode().getMessage());
                    checkRuleRequest.setErrorMessage(errorMessage);
                    APIRequest<CheckRuleRequest> requestOnException= baseHelper.getInstance(checkRuleRequest);
                    executorAfterService.execute(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                TrackingUtils.saveTrace(traceId);
                                RpcContext.getContext().set(Constant.GRAY_ENV_HEADER, envFlag);
                                FeignAsyncHelper.addHead(headMap);
                                APIResponse<CheckRuleResponse> apiResponse=ruleApi.checkException(requestOnException);
                            }catch (Exception e){
                                log.warn("requestOnException", e);
                            }
                        }
                    });
                } else {
                    log.info("not BusinessException=" , internalThrowable);
                }
            }catch (Exception e1){
                log.warn("internaldefenseException", e1);

            }


        } finally {
            // 抛出procceed()抛出的异常由ExceptionHandlerAdvice处理。
            if (throwable != null) {
                throw throwable;
            }
            return obj;
        }

    }


    private Map<String, String> getHeadersInfo(HttpServletRequest httpServletRequest) {
        Map<String, String> map = new HashMap<>();
        if (null == httpServletRequest) {
            return map;
        }
        Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = headerNames.nextElement();
            if (requestHeadSkipList.contains(key)) {
                continue;
            }
            String value = httpServletRequest.getHeader(key);
            map.put(key, value);
            if (originalHeaders.contains(key)) {
                map.put(originalHeadersPrefix + key, value);
            }
        }
        return map;
    }
}
