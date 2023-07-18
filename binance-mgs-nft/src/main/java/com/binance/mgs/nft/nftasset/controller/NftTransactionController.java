package com.binance.mgs.nft.nftasset.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.nftasset.controller.helper.AirDropHelper;
import com.binance.mgs.nft.nftasset.controller.helper.LaunchpadHelper;
import com.binance.mgs.nft.nftasset.vo.NftTransactionLpdVo;
import com.binance.nft.assetservice.enums.OperationTypeEnum;
import com.binance.nft.market.vo.airdrop.AirDropSimpleDto;
import com.binance.nft.tradeservice.dto.LaunchpadSimpleDto;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.mgs.nft.nftasset.controller.helper.PojoConvertor;
import com.binance.mgs.nft.nftasset.vo.NftTransactionArg;
import com.binance.nft.assetservice.api.INftTransactionApi;
import com.binance.nft.assetservice.api.data.request.NftTransactionDetailRequest;
import com.binance.nft.assetservice.api.data.request.NftTransactionRequest;
import com.binance.nft.assetservice.api.data.vo.NftTransactionDetailVo;
import com.binance.nft.assetservice.api.data.vo.NftTransactionVo;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Api
@Slf4j
@RequestMapping("/v1/")
@RestController
@RequiredArgsConstructor
public class NftTransactionController {

    private final INftTransactionApi nftTransactionApi;
    private final BaseHelper baseHelper;
    private final PojoConvertor pojoConvertor;
    private final LaunchpadHelper launchpadHelper;
    private final AirDropHelper airDropHelper;
    @Value("${nft.mgs.lpd.prefix:https://www.binance.com/en/nft/staking/}")
    private String LPD_PREFIX;

    @PostMapping("private/nft/nft-asset/transaction-list")
    public CommonRet<Page<NftTransactionLpdVo>> fetchNftTransactionList(@RequestBody @Valid @NotNull NftTransactionArg arg) {

        final Long userId = baseHelper.getUserId();
        if (Objects.isNull(userId)){
            new CommonRet<>();
        }

        final NftTransactionRequest nftTransactionRequest = pojoConvertor.copyNftTransactionArg(arg);
        nftTransactionRequest.setUserId(userId);
        final APIResponse<Page<NftTransactionVo>> apiResponse =
                nftTransactionApi.fetchUserNftTransactionList(APIRequest.instance(nftTransactionRequest));

        baseHelper.checkResponse(apiResponse);

        if (Objects.isNull(apiResponse.getData())
                || CollectionUtils.isEmpty(apiResponse.getData().getRecords())){
            return new CommonRet<>();
        }
        final List<NftTransactionVo> records = apiResponse.getData().getRecords();

        final Page<NftTransactionLpdVo> resultPage = pojoConvertor.copyTransactionLpd(apiResponse.getData());
        if (arg.getOperations().contains(OperationTypeEnum.AIR_DROP.getCode())){

            final List<AirDropSimpleDto> airDropSimpleDtos =
                    airDropHelper.fetchAirdropDesc(records.stream().map(NftTransactionVo::getNftId).collect(Collectors.toList()));

            resultPage.getRecords().forEach(x -> {
                final AirDropSimpleDto airDropSimpleDto =
                        airDropSimpleDtos.stream().filter(e -> e.getNftId().equals(x.getNftId())).findFirst().orElseGet(AirDropSimpleDto::new);
                x.setRemark(airDropSimpleDto.getDescription());
                x.setOperation(OperationTypeEnum.AIR_DROP.getCode());
            });
        }else {
            final List<LaunchpadSimpleDto> lpdDtos =
                    launchpadHelper.queryByNftInfoIds(records.stream().map(NftTransactionVo::getNftId).collect(Collectors.toList()));

            resultPage.getRecords().forEach(x -> {
                final LaunchpadSimpleDto launchpadSimpleDto = lpdDtos.stream().filter(e -> e.getNftId().equals(x.getNftId())).findFirst().orElse(null);
                if (Objects.isNull(launchpadSimpleDto)) return;
                x.setEventName(launchpadSimpleDto.getEventName());
                x.setPageLink(LPD_PREFIX + launchpadSimpleDto.getPageLink());
                x.setOperation(OperationTypeEnum.NFT_PLEDGED_DISTRIBUTE.getCode());
            });
        }
        return new CommonRet<>(resultPage);
    }

    @PostMapping("private/nft/nft-asset/transaction-detail")
    public CommonRet<Page<NftTransactionDetailVo>> fetchNftTransactionDetail(@RequestBody @Valid @NotNull NftTransactionArg arg) {

        final Long userId = baseHelper.getUserId();
        if (Objects.isNull(userId)){
            new CommonRet<>();
        }

        final NftTransactionDetailRequest nftTransactionDetailRequest = new NftTransactionDetailRequest();

        nftTransactionDetailRequest.setUserId( userId );
        nftTransactionDetailRequest.setPage( arg.getPage() );
        nftTransactionDetailRequest.setRows( arg.getRows() );
        nftTransactionDetailRequest.setStartTime( arg.getStartTime() );
        nftTransactionDetailRequest.setEndTime( arg.getEndTime() );
        nftTransactionDetailRequest.setTransactionId(arg.getTransactionId());
        nftTransactionDetailRequest.setOperations(arg.getOperations());
        final APIResponse<Page<NftTransactionDetailVo>> apiResponse =
                nftTransactionApi.fetchUserNftTransactionDetail(APIRequest.instance(nftTransactionDetailRequest));

        baseHelper.checkResponse(apiResponse);

        return new CommonRet<>(apiResponse.getData());
    }
}
