package com.binance.mgs.nft.payment;

import com.alibaba.fastjson.JSON;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.JsonUtils;
import com.binance.mgs.nft.payment.request.PaymentEncryptionRequest;
import com.binance.nft.fantoken.response.binancepay.WebhookOrderNotificationResponse;
import com.binance.nft.paymentservice.api.iface.IPaymentChannelApi;
import com.binance.nft.paymentservice.api.request.BatchTradeEncryptionRequest;
import com.binance.nft.paymentservice.api.response.BaseNotificationResponse;
import com.binance.nft.paymentservice.api.response.EncryptionResponse;
import com.binance.nft.paymentservice.api.request.EncryptionRequest;
import com.binance.nft.paymentservice.dto.PayNotificationDto;
import com.binance.nft.paymentservice.enums.PaymentChannelEnum;
import com.binance.nft.paymentservice.enums.PaymentCodeEnum;
import com.binance.nft.tradeservice.api.IPreOrderApi;
import com.binance.nft.tradeservice.request.preorder.PreOrderCheckOrderRequest;
import com.binance.nft.tradeservice.response.PreOrderCheckOrderResponse;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class PaymentController {

    private final BaseHelper baseHelper;
    private final IPaymentChannelApi paymentChannelApi;

    private final IPreOrderApi preOrderApi;
    /**
     * callback
     * */
    @PostMapping("/friendly/nft/payment/order-notification")
    public void preOrderNotification(
            @RequestHeader("BinancePay-Timestamp") String timestamp,
            @RequestHeader("BinancePay-Nonce") String nonce,
            @RequestHeader("BinancePay-Signature") String signature,
            @RequestBody String body, HttpServletResponse response) {
        this.binancePayNotification(timestamp, nonce, signature, body,
                PaymentCodeEnum.TRANSFER_BY_CASHIER.getCode(), response);
    }

    @SneakyThrows
    private void binancePayNotification(String timestamp, String nonce, String signature,
            String body, Integer paymentCode, HttpServletResponse response) {
        PayNotificationDto notification = PayNotificationDto.builder()
                .channelCode(PaymentChannelEnum.BINANCE_PAY.getCode())
                .paymentCode(paymentCode)
                .timestamp(timestamp)
                .nonce(nonce)
                .signature(signature)
                .body(body)
                .build();

        APIResponse<BaseNotificationResponse> responseAPIResponse =
                paymentChannelApi.payNotification(APIRequest.instance(notification));
        baseHelper.checkResponse(responseAPIResponse);

        WebhookOrderNotificationResponse result = WebhookOrderNotificationResponse.builder()
                .returnCode(responseAPIResponse.getData().getReturnCode())
                .returnMessage(responseAPIResponse.getData().getReturnMessage())
                .build();

        ServletOutputStream outputStream = response.getOutputStream();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        outputStream.write(JSON.toJSONString(result).getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();
    }


    @PostMapping({"/private/nft/nft-trade/encryption", "/private/nft/payment/encryption"})
    public CommonRet<EncryptionResponse> encryption(@RequestBody PaymentEncryptionRequest req) throws Exception {
        log.error("encryption json request = " + JsonUtils.toJsonHasNullKey(req));
        if(baseHelper.getUserId() == null) {
            return new CommonRet<>();
        }
        PreOrderCheckOrderRequest build = PreOrderCheckOrderRequest.builder().productId(req.getProductId()).nftId(CollectionUtils.isEmpty(req.getNftIds()) ? null : req.getNftIds().get(0)).requestId(Long.parseLong(req.getMerchantOrderId())).userId(baseHelper.getUserId()).build();

        APIResponse<PreOrderCheckOrderResponse> orderResponse = preOrderApi.checkPreOrders(APIRequest.instance(build));
        baseHelper.checkResponse(orderResponse);
        APIResponse<EncryptionResponse> response = paymentChannelApi.encryption(APIRequest.instance(req));
        baseHelper.checkResponse(response);
        return new CommonRet<EncryptionResponse>(response.getData());
    }

    @PostMapping({"/private/nft/nft-trade/batch-trade-encryption"})
    public CommonRet<EncryptionResponse> BatchTradeEncryption(@RequestBody BatchTradeEncryptionRequest req) {
        if (baseHelper.getUserId() == null) {
            return new CommonRet<>();
        }
        long expireAfterMs = 10 * 60 * 1000L;
        EncryptionRequest encryptionRequest = CopyBeanUtils.fastCopy(req, EncryptionRequest.class);
        encryptionRequest.setMerchantOrderId(String.valueOf(req.getBatchId()));
        encryptionRequest.setExpireTime(System.currentTimeMillis() + expireAfterMs);
        APIResponse<EncryptionResponse> response = paymentChannelApi.encryption(APIRequest.instance(encryptionRequest));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }
}
