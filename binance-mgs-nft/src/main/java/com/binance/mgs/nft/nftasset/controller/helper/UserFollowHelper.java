package com.binance.mgs.nft.nftasset.controller.helper;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.nft.assetservice.api.data.request.follow.BatchFollowListQuery;
import com.binance.nft.assetservice.api.data.response.follow.BatchFollowItem;
import com.binance.nft.assetservice.api.follow.IUserFollowApi;
import com.binance.platform.mgs.base.helper.BaseHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserFollowHelper {

    private final IUserFollowApi userFollowApi;

    private final BaseHelper baseHelper;

    public Map<Long, Boolean> queryFollow(List<Long> creatorIds, Long userId) {
        if(Objects.isNull(userId) || CollectionUtils.isEmpty(creatorIds)) return Collections.EMPTY_MAP;
        APIResponse<List<BatchFollowItem>> response = userFollowApi.batchUserFollowInfo(APIRequest.instance(BatchFollowListQuery.builder()
                .followIdList(creatorIds).userId(userId).build()));
        baseHelper.checkResponse(response);
        return response.getData().stream().collect(Collectors.toMap(BatchFollowItem::getUserId, BatchFollowItem::isFollow));
    }
}
