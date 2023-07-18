package com.binance.mgs.nft.fantoken.controller;

import com.alibaba.fastjson.JSON;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.nft.fantoken.request.fanshop.FanShopFiatCheckoutRequest;
import com.binance.nft.fantoken.request.fanshop.RoundNftOrderStatusRequest;
import com.binance.nft.fantoken.response.fanshop.FanShopFiatCheckoutResponse;
import com.binance.master.utils.IPUtils;
import com.binance.master.utils.WebUtils;
import com.binance.nft.fantoken.ifae.fanshop.IFanShopFiatOrderApi;
import com.binance.nft.fantoken.request.FanTokenBaseRequest;
import com.binance.nft.fantoken.request.fanshop.AcceptFiatOrderRequest;
import com.binance.nft.fantoken.response.fanshop.FanShopAlpineTicketResponse;
import com.binance.nft.fantoken.response.fanshop.RoundNftOrderStatusResponse;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.mgs.nft.fantoken.helper.FanTokenCheckHelper;
import com.binance.mgs.nft.mysterybox.helper.FanTokenI18nHelper;
import com.binance.nft.fantoken.ifae.IFanShopItemManageAPI;
import com.binance.nft.fantoken.ifae.IFanShopOrderManageApi;
import com.binance.nft.fantoken.ifae.IFanShopOrderUserFormManageApi;
import com.binance.nft.fantoken.ifae.IFanShopSubscriptionConfigManageAPI;
import com.binance.nft.fantoken.request.CommonPageRequest;
import com.binance.nft.fantoken.request.binancepay.WebhookOrderNotificationRequest;
import com.binance.nft.fantoken.request.fanshop.AcceptOrderRequest;
import com.binance.nft.fantoken.request.fanshop.CreateOrderUserFormRequest;
import com.binance.nft.fantoken.request.fanshop.DisplayItemInfoRequest;
import com.binance.nft.fantoken.request.fanshop.DisplayPageItemRequest;
import com.binance.nft.fantoken.request.fanshop.FanShopCheckoutRequest;
import com.binance.nft.fantoken.request.fanshop.FanShopSubscriptionRequest;
import com.binance.nft.fantoken.request.fanshop.OrderDetailRequest;
import com.binance.nft.fantoken.request.fanshop.OrderIdRequest;
import com.binance.nft.fantoken.response.CommonPageResponse;
import com.binance.nft.fantoken.response.VoidResponse;
import com.binance.nft.fantoken.response.binancepay.WebhookOrderNotificationResponse;
import com.binance.nft.fantoken.response.fanshop.FanShopCheckoutResponse;
import com.binance.nft.fantoken.response.fanshop.FanShopItemResponse;
import com.binance.nft.fantoken.response.fanshop.OrderDetailResponse;
import com.binance.nft.fantoken.response.fanshop.OrderIdResponse;
import com.binance.nft.fantoken.response.fanshop.OrderUserFormResponse;
import com.binance.nft.fantoken.response.fanshop.PaymentInfoResponse;
import com.binance.nft.fantoken.response.fanshop.SimpleItemInfo;
import com.binance.nft.fantoken.vo.fanshop.WebhookOrderNotificationVO;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/**
 * <h1>FanShop Controller</h1>
 * */
@SuppressWarnings("all")
@Slf4j
@RequestMapping("/v1/friendly/nft/fantoken/fanshop")
@RestController
@RequiredArgsConstructor
public class FanTokenFanShopController {

    private final BaseHelper baseHelper;
    private final FanTokenI18nHelper fanTokenI18nHelper;
    private final FanTokenCheckHelper fanTokenCheckHelper;
    private final IFanShopSubscriptionConfigManageAPI fanShopSubscriptionConfigManageAPI;
    private final IFanShopItemManageAPI fanShopItemManageAPI;
    private final IFanShopOrderManageApi fanShopOrderManageApi;
    private final IFanShopOrderUserFormManageApi fanShopOrderUserFormManageApi;
    private final IFanShopFiatOrderApi fanShopFiatOrderApi;

    /**
     * <h2>订阅商品</h2>
     * 需要注意:
     *  1. 需要用户登录
     *  2. 需要通过 KYC 校验
     * */
    @PostMapping("/subscription")
    public CommonRet<VoidResponse> itemSubscription(@RequestBody FanShopSubscriptionRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId || null == request.getItemId()) {
            return new CommonRet<>(new VoidResponse());
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.setUserId(userId);
        request.setComplianceAssetDto(fanTokenCheckHelper.fanTokenComplianceAsset(userId));
        // 用户订阅
        APIResponse<VoidResponse> response =
                fanShopSubscriptionConfigManageAPI.itemSubscription(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>shop page</h2>
     * */
    @PostMapping("/item-list")
    public CommonRet<CommonPageResponse<FanShopItemResponse>> queryPageItemInfo(
            @RequestBody CommonPageRequest<DisplayPageItemRequest> request) {

        Long userId = baseHelper.getUserId();
        if (null != userId) {
            request.getParams().setUserId(userId);
        }

        // 限制一页最多返回 20 条数据
        if (request.getSize() <= 0 || request.getSize() > 20) {
            request.setSize(20);
        }

        request.getParams().setComplianceAssetDto(fanTokenCheckHelper.fanTokenComplianceAsset(userId));
        APIResponse<CommonPageResponse<FanShopItemResponse>> response =
                fanShopItemManageAPI.queryPageItemInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        // i18n
        fanTokenI18nHelper.doPageFanShopItemInfo(response.getData().getData());
        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>item detail</h2>
     * */
    @PostMapping("/item-info")
    public CommonRet<FanShopItemResponse> queryDisplayItemInfo(@RequestBody DisplayItemInfoRequest request) {

        Long userId = baseHelper.getUserId();
        if (null != userId) {
            request.setUserId(userId);
        }

        request.setComplianceAssetDto(fanTokenCheckHelper.fanTokenComplianceAsset(userId));
        APIResponse<FanShopItemResponse> response = fanShopItemManageAPI.queryDisplayItemInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        // i18n
        fanTokenI18nHelper.doFanShopItemInfo(response.getData());
        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>商品结算页</h2>
     * */
    @PostMapping("/checkout")
    public CommonRet<FanShopCheckoutResponse> itemCheckout(@RequestBody FanShopCheckoutRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.setUserId(userId);
        request.setHasKyc(Boolean.TRUE);
        request.setComplianceAssetDto(fanTokenCheckHelper.fanTokenComplianceAsset(userId));

        // 商品结算
        APIResponse<FanShopCheckoutResponse> response =
                fanShopOrderManageApi.itemCheckout(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        // i18n
        fanTokenI18nHelper.doItemCheckout(response.getData());
        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>预创建订单</h2>
     * 由于创建 BinancePay Order 订单需要 productName, 需要在 MGS 中填充 ItemName
     * */
    @PostMapping("/create-order")
    public CommonRet<OrderIdResponse> acceptOrder(@RequestBody AcceptOrderRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 合规校验
        boolean isNonComplianceUser = !fanTokenCheckHelper.membershipComplianceCheck(userId);
        if (isNonComplianceUser) {
            log.error("fantoken user is non compliance user (in acceptFanShopOrder): [userId={}], [isNonComplianceUser={}]",
                    userId, isNonComplianceUser);
            return new CommonRet<>();
        }

        // risk 需要的参数
        request.setFvideoId(WebUtils.getHeader("fvideo-id"));
        request.setIp(IPUtils.getIp());
        request.setClientType(baseHelper.getClientType());

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.setUserId(userId);
        request.setHasKyc(Boolean.TRUE);
        request.setLanguage(baseHelper.getLanguage());

        // 填充商品的 itemName 和 itemNameKey
        String itemId = request.getItemInfos().get(0).getItemId();
        APIResponse<SimpleItemInfo> itemInfoResponse =
                fanShopItemManageAPI.querySimpleItemInfo(APIRequest.instance(itemId));
        baseHelper.checkResponse(itemInfoResponse);
        fanTokenI18nHelper.doAcceptOrderRequest(request, itemInfoResponse.getData());

        // gcc compliance
        request.setComplianceAssetDto(fanTokenCheckHelper.fanTokenComplianceAsset(userId));

        // 预创建订单
        APIResponse<OrderIdResponse> response =
                fanShopOrderManageApi.acceptOrder(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>用户手动取消订单</h2>
     * */
    @PostMapping("/cancel-order")
    public CommonRet<PaymentInfoResponse> cancelOrder(@RequestBody OrderIdRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.setUserId(userId);

        // 用户手动取消订单
        APIResponse<PaymentInfoResponse> response =
                fanShopOrderManageApi.cancelOrder(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>获取订单的支付信息</h2>
     * */
    @PostMapping("/payment")
    public CommonRet<PaymentInfoResponse> genPaymentInfo(@RequestBody OrderIdRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.setUserId(userId);

        // 获取订单的支付信息
        APIResponse<PaymentInfoResponse> response =
                fanShopOrderManageApi.genPaymentInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>单个订单的详情</h2>
     * */
    @PostMapping("/order-info")
    public CommonRet<OrderDetailResponse> orderDetail(@RequestBody OrderDetailRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        request.setUserId(userId);

        // 单个订单的详情
        APIResponse<OrderDetailResponse> response =
                fanShopOrderManageApi.orderDetail(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        // i18n
        fanTokenI18nHelper.doOrderDetail(response.getData());
        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>订单历史页面</h2>
     * */
    @PostMapping("/order-infos")
    public CommonRet<CommonPageResponse<OrderDetailResponse>> pageOrderDetail(@RequestBody CommonPageRequest<OrderDetailRequest> request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 限制一页最多返回 20 条数据
        if (request.getSize() <= 0 || request.getSize() > 10) {
            request.setSize(10);
        }

        request.getParams().setUserId(userId);

        // 订单历史页面
        APIResponse<CommonPageResponse<OrderDetailResponse>> response =
                fanShopOrderManageApi.pageOrderDetail(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        // i18n
        fanTokenI18nHelper.doPageOrderDetail(response.getData().getData());
        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>接收 BinancePay Order Notification Webhook, 通知订单的状态: PAY_SUCCESS, PAY_CLOSED</h2>
     * */
    @SneakyThrows
    @PostMapping("/order-notification")
    public void receiveOrderNotification(@RequestHeader("BinancePay-Timestamp") String timestamp,
                                         @RequestHeader("BinancePay-Nonce") String nonce,
                                         @RequestHeader("BinancePay-Signature") String signature,
                                         @RequestBody String body,
                                         HttpServletResponse response) {

        WebhookOrderNotificationVO notificationVO = WebhookOrderNotificationVO.builder()
                .timestamp(timestamp)
                .nonce(nonce)
                .signature(signature)
                .body(body)
                .request(JSON.parseObject(body, WebhookOrderNotificationRequest.class))
                .build();

        APIResponse<WebhookOrderNotificationResponse> notification =
                fanShopOrderManageApi.receiveOrderNotification(notificationVO);
        baseHelper.checkResponse(notification);

        WebhookOrderNotificationResponse result = WebhookOrderNotificationResponse.builder()
                .returnCode(notification.getData().getReturnCode())
                .returnMessage(notification.getData().getReturnMessage())
                .build();

        ServletOutputStream outputStream = response.getOutputStream();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        outputStream.write(JSON.toJSONString(result).getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();
    }

    /**
     * <h2>接收 FIAT CARD Notification Webhook 修改订单状态</h2>
     */
    @SneakyThrows
    @PostMapping("/card-order-notification")
    public void receiveCardOrderNotification(@RequestHeader("FiatCard-Timestamp") String timestamp,
                                         @RequestHeader("FiatCard-Nonce") String nonce,
                                         @RequestHeader("FiatCard-Signature") String signature,
                                         @RequestBody String body,
                                         HttpServletResponse response) {

        WebhookOrderNotificationVO notificationVO = WebhookOrderNotificationVO.builder()
                .timestamp(timestamp)
                .nonce(nonce)
                .signature(signature)
                .body(body)
                .request(JSON.parseObject(body, WebhookOrderNotificationRequest.class))
                .build();

        APIResponse<WebhookOrderNotificationResponse> notification = fanShopFiatOrderApi.receiveOrderNotification(APIRequest.instance(notificationVO));
        WebhookOrderNotificationResponse result = WebhookOrderNotificationResponse.builder()
                .returnCode(notification.getData().getReturnCode())
                .returnMessage(notification.getData().getReturnMessage())
                .build();

        ServletOutputStream outputStream = response.getOutputStream();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        outputStream.write(JSON.toJSONString(result).getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();
    }

    /**
     * <h2>提交订单用户信息表单</h2>
     * 场景: 1.下单 2.订单历史
     * */
    @PostMapping("/submit-form")
    public CommonRet<VoidResponse> submitOrderUserForm(@RequestBody CreateOrderUserFormRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 取消 KYC 的校验 Fiat&BinancePay 共用，Fiat不需要KYC校验

        request.setUserId(userId);

        // 提交用户表单
        APIResponse<VoidResponse> response = fanShopOrderUserFormManageApi.submitOrderUserForm(
                APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>查看订单用户信息表单</h2>
     * */
    @PostMapping("/view-form")
    public CommonRet<OrderUserFormResponse> viewOrderUserForm(@RequestBody OrderIdRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 取消 KYC 的校验 Fiat&BinancePay 共用，Fiat不需要KYC校验

        request.setUserId(userId);

        // 提交用户表单
        APIResponse<OrderUserFormResponse> response = fanShopOrderUserFormManageApi.viewOrderUserForm(
                APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/create-fiat-order")
    public CommonRet<OrderIdResponse> acceptFiatOrder(@RequestBody AcceptFiatOrderRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 强制 KYC 的校验, 合规校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.setUserId(userId);
        request.setLanguage(baseHelper.getLanguage());

        // risk 需要的参数
        request.setFvideoId(WebUtils.getHeader("fvideo-id"));
        request.setIp(IPUtils.getIp());
        request.setClientType(baseHelper.getClientType());
        // gcc compliance
        request.setComplianceAssetDto(fanTokenCheckHelper.fanTokenComplianceAsset(userId));

        // 预创建订单
        APIResponse<OrderIdResponse> response =
                fanShopFiatOrderApi.acceptFiatOrder(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/query-alpine-ticket-info")
    public CommonRet<FanShopAlpineTicketResponse> queryAlpineTicketInfo(@RequestBody FanTokenBaseRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        request.setUserId(userId);

        // 查询用户 Alpine Ticket 信息
        APIResponse<FanShopAlpineTicketResponse> response = fanShopItemManageAPI.queryAlpineTicketInfo(
                APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/fiat-checkout")
    public CommonRet<FanShopFiatCheckoutResponse> fiatPayItemCheckout(@RequestBody FanShopFiatCheckoutRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 强制 KYC 的校验, 合规校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.setUserId(userId);
        // gcc compliance
        request.setComplianceAssetDto(fanTokenCheckHelper.fanTokenComplianceAsset(userId));

        // 商品结算
        APIResponse<FanShopFiatCheckoutResponse> response =
                fanShopFiatOrderApi.fiatPayItemCheckout(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        // i18n
        fanTokenI18nHelper.doFiatItemCheckout(response.getData());
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/round-nft-order-status")
    public CommonRet<RoundNftOrderStatusResponse> roundNftOrderStatus(@RequestBody RoundNftOrderStatusRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 取消 KYC 的校验

        request.setUserId(userId);

        // 轮询 NFT 分发
        APIResponse<RoundNftOrderStatusResponse> response = fanShopOrderManageApi.roundNftOrderStatus(APIRequest
                .instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }
}
