package com.binance.mgs.account.api.vo;

import com.binance.accountapimanage.vo.apimanage.request.ResetApiModeRequest;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

/**
 * @author sean w
 * @date 2022/9/1
 **/
@ApiModel
@Data
public class ResetEnableTradeTimeArg {

    List<ResetApiModeRequest> resetApiModeRequests;
}
