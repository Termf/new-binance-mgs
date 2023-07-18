package com.binance.mgs.nft;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetBucketEncryptionResult;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.trade.proxy.TradeCacheProxy;
import com.binance.nft.mintservice.api.iface.NFTMintApi;
import com.binance.nft.mintservice.api.vo.NFTMintUploadRequest;
import com.binance.nft.mintservice.api.vo.NFTMinteUploadResponse;
import com.binance.nft.tradeservice.request.ProductFeeRequest;
import com.binance.nft.tradeservice.vo.ProductFeeVo;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = BinanceNFTMgsApplication.class)
public class MintServiceTest {

    @Autowired
    private AmazonS3 amazonS3;
    @Autowired
    private AWSLambda awsLambda;
    @Autowired
    private AmazonRekognition amazonRekognition;
    @Autowired
    private TradeCacheProxy tradeCacheProxy;

    @Autowired
    private NFTMintApi nftMintApi;

    @Test
    public void testOrder() throws Exception {
        NFTMintUploadRequest request = new NFTMintUploadRequest();
        request.setUserId("112233");
        request.setSuffix(".json");
        APIResponse<NFTMinteUploadResponse> response = nftMintApi.genUploadUrl(APIRequest.instance(request));
         response = nftMintApi.genUploadUrl(APIRequest.instance(request));
        System.out.println();
    }

    @Test
    public void testAmazonRekognition() {
        GetBucketEncryptionResult bucketEncryption = amazonS3.getBucketEncryption("");
        System.out.println(bucketEncryption);
    }

    @SneakyThrows
    @Test
    public void testFee() {
        ProductFeeRequest request = new ProductFeeRequest();
        request.setSource(1);
        request.setCreatorId(354830128L);
        ProductFeeVo f1 = tradeCacheProxy.onsaleFee(request);
        request.setCreatorId(3548301281L);

        ProductFeeVo f2 = tradeCacheProxy.onsaleFee(request);
        request.setCreatorId(354830128L);

        ProductFeeVo f3 = tradeCacheProxy.onsaleFee(request);
        System.out.println();
    }

    public static void main(String[] args) {
        HashMap<String, Double> map = Maps.newHashMap();
        map.put("BAYC",0.21D);
        map.put("MAYC",0.21D);
        map.put("BAkC",0.21D);
        System.out.println(new CommonRet<>(map));
    }
}
