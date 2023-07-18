package com.binance.mgs.nft;

import com.binance.mgs.nft.fantoken.helper.FanTokenBTSHelper;
import com.binance.platform.mgs.base.helper.CrowdinHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = BinanceNFTMgsApplication.class)
public class CrowinHelperTest {

    @Resource
    private CrowdinHelper crowdinHelper;
    @Resource
    private FanTokenBTSHelper fanTokenBTSHelper;

    @Test
    public void testCrowdin() throws Exception {
        String msg = crowdinHelper.getMessageByKey("nft-binance-introduction", "en");
        System.out.println();
    }

    @Test
    public void testBTS() {
        String messageByKey = fanTokenBTSHelper.getMessageByKey("fan-token-desc-test-1", "zh-TW");
        System.out.println(messageByKey);
    }
}
