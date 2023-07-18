package com.binance.mgs.nft.deposit;

import com.binance.nft.cex.wallet.api.deposit.admin.IMetaRefreshAdminAPI;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author: allen.f
 * @date: 2022/1/20
 **/
@RequiredArgsConstructor
@RestController
public class MetaRefreshController {

    private final IMetaRefreshAdminAPI metaRefreshAPI;

    @GetMapping("/v1/public/nft/check/in/refresh")
    CommonRet<Boolean> checkIfInRefresh(@RequestParam("networkType") String networkType,
                                        @RequestParam("contractAddress") String contractAddress,
                                        @RequestParam("tokenId") String tokenId){

        return new CommonRet<>(metaRefreshAPI.checkIfInRefresh(networkType, contractAddress, tokenId).getData());
    }
}
