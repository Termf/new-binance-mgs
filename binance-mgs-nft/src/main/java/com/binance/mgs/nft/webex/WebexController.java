package com.binance.mgs.nft.webex;

import com.binance.mgs.nft.webex.request.WebexRequest;
import com.binance.mgs.nft.webex.service.WebexService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api
@Slf4j
@RestController
@RequestMapping("/v1")
public class WebexController {

    @Autowired
    private WebexService webexService;

    @PostMapping("public/nft/postWebex")
    public void postWebex(@RequestBody WebexRequest request) {
        log.info("postwebex: {}",request);
        webexService.postWebex(request);
    }
}
