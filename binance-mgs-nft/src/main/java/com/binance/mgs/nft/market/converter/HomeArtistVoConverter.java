package com.binance.mgs.nft.market.converter;

import com.binance.mgs.nft.market.utils.AesUtil;
import com.binance.mgs.nft.market.vo.HomeArtistMgsVo;
import com.binance.nft.market.vo.CommonPageResponse;
import com.binance.nft.market.vo.artist.HomeArtistVo;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;


public class HomeArtistVoConverter {


    public static CommonPageResponse<HomeArtistMgsVo> convert(CommonPageResponse<HomeArtistVo> response, String password) {

        List<HomeArtistMgsVo> homeArtistMgsVoList = new ArrayList<>(response.getData().size());

        response.getData().forEach(
                item -> {
                    HomeArtistMgsVo homeArtistMgsVo = new HomeArtistMgsVo();
                    BeanUtils.copyProperties(item, homeArtistMgsVo);
                    if (item.getUserId() != null){
                        homeArtistMgsVo.setCreatorId(item.getUserId());
                        homeArtistMgsVo.setUserId(AesUtil.encrypt(item.getUserId().toString(), password));
                    }
                    homeArtistMgsVo.setFollowRelation(0);
                    homeArtistMgsVoList.add(homeArtistMgsVo);
                }
        );
        CommonPageResponse<HomeArtistMgsVo> commonPageResponse = new CommonPageResponse<>(response.getPage(), response.getSize(), response.getTotal(), homeArtistMgsVoList);

        return commonPageResponse;
    }
}
