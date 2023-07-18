package com.binance.mgs.nft.mysterybox.helper;

import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CrowdinHelper;
import com.binance.nft.mystery.api.vo.MysteryBoxProductDetailVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MysteryBoxI18nHelper {


    private final CrowdinHelper crowdinHelper;

    private final BaseHelper baseHelper;


    public void doI18n(MysteryBoxProductDetailVo mysteryBoxProductDetailIncludeAssetVo) {
        mysteryBoxProductDetailIncludeAssetVo.setDescription(crowdinHelper.getMessageByKey(mysteryBoxProductDetailIncludeAssetVo.getDescription(), baseHelper.getLanguage()));
        mysteryBoxProductDetailIncludeAssetVo.setName(crowdinHelper.getMessageByKey(mysteryBoxProductDetailIncludeAssetVo.getName(), baseHelper.getLanguage()));
        mysteryBoxProductDetailIncludeAssetVo.setSubTitle(crowdinHelper.getMessageByKey(mysteryBoxProductDetailIncludeAssetVo.getSubTitle(), baseHelper.getLanguage()));
        mysteryBoxProductDetailIncludeAssetVo.setArtist(crowdinHelper.getMessageByKey(mysteryBoxProductDetailIncludeAssetVo.getArtist(), baseHelper.getLanguage()));
        mysteryBoxProductDetailIncludeAssetVo.setRules(crowdinHelper.getMessageByKey(mysteryBoxProductDetailIncludeAssetVo.getRules(), baseHelper.getLanguage()));
    }


}
