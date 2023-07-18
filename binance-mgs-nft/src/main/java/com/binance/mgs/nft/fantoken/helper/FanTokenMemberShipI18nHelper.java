package com.binance.mgs.nft.fantoken.helper;

import com.binance.nft.fantoken.response.CommonPageResponse;
import com.binance.nft.fantoken.response.membership.MemberShipTaskResponse;
import com.binance.nft.fantoken.response.membership.MemberShipTierInfoResponse;
import com.binance.nft.fantoken.response.membership.MemberShipUserTierInfoResponse;
import com.binance.nft.fantoken.vo.membership.MemberShipRewardInfo;
import com.binance.nft.fantoken.vo.membership.MemberShipTaskInfo;
import com.binance.platform.mgs.base.helper.BaseHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.Objects;

@SuppressWarnings("all")
@Component
@RequiredArgsConstructor
public class FanTokenMemberShipI18nHelper {

    private final BaseHelper baseHelper;
    private final FanTokenBTSHelper fanTokenBTSHelper;

    public void doTierInfo(MemberShipTierInfoResponse response) {

        if (null != response && CollectionUtils.isNotEmpty(response.getTierInfos())) {
            response.getTierInfos().forEach(t -> {
                t.setName(fanTokenBTSHelper.getMessageByKey(t.getNameKey(), baseHelper.getLanguage()));
                t.setDescription(fanTokenBTSHelper.getMessageByKey(t.getDescriptionKey(), baseHelper.getLanguage()));
            });
        }
    }

    public void doUserInfo(MemberShipUserTierInfoResponse response) {

        if (Objects.nonNull(response) && Objects.nonNull(response.getUserInfo())) {
            response.getUserInfo().setLevelName(fanTokenBTSHelper.getMessageByKey(response.getUserInfo().getLevelNameKey(), baseHelper.getLanguage()));
            response.getUserInfo().setLevelDescription(fanTokenBTSHelper.getMessageByKey(response.getUserInfo().getLevelDescriptionKey(), baseHelper.getLanguage()));
            response.getUserInfo().setNextLevelName(fanTokenBTSHelper.getMessageByKey(response.getUserInfo().getNextLevelNameKey(), baseHelper.getLanguage()));
            response.getUserInfo().setNextLevelDescription(fanTokenBTSHelper.getMessageByKey(response.getUserInfo().getNextLevelDescriptionKey(), baseHelper.getLanguage()));
        }
    }

    public void doTaskCenter(CommonPageResponse<MemberShipTaskInfo> response) {

        if (null != response && CollectionUtils.isNotEmpty(response.getData())) {
            response.getData().forEach(this::doMemberShipTaskInfo);
        }
    }

    private void doMemberShipTaskInfo(MemberShipTaskInfo taskInfo) {

        if (null != taskInfo) {
            taskInfo.setTaskName(fanTokenBTSHelper.getMessageByKey(taskInfo.getTaskNameKey(), baseHelper.getLanguage()));
            taskInfo.setTaskDescription(fanTokenBTSHelper.getMessageByKey(taskInfo.getTaskDescriptionKey(), baseHelper.getLanguage()));
            taskInfo.setButtonName(fanTokenBTSHelper.getMessageByKey(taskInfo.getButtonNameKey(), baseHelper.getLanguage()));
        }
    }

    public void doLandingPageTask(MemberShipTaskResponse response) {

        if (null != response && CollectionUtils.isNotEmpty(response.getTasks())) {
            response.getTasks().forEach(this::doMemberShipTaskInfo);
        }
    }

    public void doRewardPool(CommonPageResponse<MemberShipRewardInfo> response) {

        if (null != response && CollectionUtils.isNotEmpty(response.getData())) {
            response.getData().forEach(r -> {
                r.setItemName(fanTokenBTSHelper.getMessageByKey(r.getItemNameKey(), baseHelper.getLanguage()));
                r.setItemDescription(fanTokenBTSHelper.getMessageByKey(r.getItemDescriptionKey(), baseHelper.getLanguage()));
            });
        }
    }
}
