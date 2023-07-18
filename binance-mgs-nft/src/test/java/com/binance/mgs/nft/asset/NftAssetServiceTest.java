package com.binance.mgs.nft.asset;

import com.binance.mgs.nft.BinanceNFTMgsApplication;
import com.binance.mgs.nft.nftasset.controller.helper.NftAssetHelper;
import com.binance.mgs.nft.nftasset.response.NftAssetDetailResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = BinanceNFTMgsApplication.class)
public class NftAssetServiceTest {

    @Autowired
    private NftAssetHelper nftAssetHelper;


    @Test
    public void testOrder() throws Exception {
        NftAssetDetailResponse response = nftAssetHelper.queryNftAssetDetailByProductId(null, 808607L);
        System.out.println();
    }
}
