package com.binance.mgs.nft.activity.controller;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.nft.activityservice.api.LeaderBoardApi;
import com.binance.nft.activityservice.request.LeaderBoardUserJoinRequest;
import com.binance.nft.activityservice.response.LeaderBoardInfoResponse;
import com.binance.nft.activityservice.response.LeaderBoardRankResponse;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Api
@Slf4j
@RestController
@RequestMapping("/v1")
public class LeaderBoardController {

    @Resource
    private LeaderBoardApi leaderBoardApi;
    @Resource
    private BaseHelper baseHelper;


    @GetMapping(value = "/public/nft/activity/leaderboard/info")
    public CommonRet<LeaderBoardInfoResponse> leaderBoardInfo(@RequestParam("leaderboardId") Long leaderboardId) throws Exception {
        APIResponse<LeaderBoardInfoResponse> response = leaderBoardApi.leaderBoardInfo(leaderboardId);
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping({"/private/nft/activity/leaderboard/join"})
    public CommonRet<Void> userJoin(@RequestBody LeaderBoardUserJoinRequest request) {
        APIResponse<Void> response = leaderBoardApi.userJoin(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>();
    }

    @GetMapping({"/friendly/nft/activity/leaderboard/rank"})
    public CommonRet<LeaderBoardRankResponse> rank(@RequestParam("leaderboardId") Long leaderboardId, @RequestParam("page") Integer page, @RequestParam("size") Integer size) {
        APIResponse<LeaderBoardRankResponse> response = leaderBoardApi.rank(leaderboardId, page, size);
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

}
