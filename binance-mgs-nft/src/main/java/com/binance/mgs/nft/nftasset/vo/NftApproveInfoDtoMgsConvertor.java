package com.binance.mgs.nft.nftasset.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.mgs.nft.market.utils.AesUtil;
import com.binance.mgs.nft.market.vo.HomeArtistMgsVo;
import com.binance.nft.assetservice.api.data.response.NftApproveInfoDto;
import com.binance.nft.common.utils.ObjectUtils;
import com.binance.nft.market.vo.CommonPageResponse;

import java.util.ArrayList;
import java.util.List;

public class NftApproveInfoDtoMgsConvertor {


    public static APIResponse<Page<NftApproveInfoMgsDto>> convert(APIResponse<Page<NftApproveInfoDto>> response, String password) {

        List<NftApproveInfoMgsDto> mgsDtoList = new ArrayList<>(response.getData().getRecords().size());

        response.getData().getRecords().forEach(item ->{
            NftApproveInfoMgsDto mgsDto = CopyBeanUtils.fastCopy(item, NftApproveInfoMgsDto.class);
            if (!ObjectUtils.isEmpty(item.getCreator()) && item.getCreator().getUserId() != null){
                UserSimpleInfoMgsDto userSimpleInfoMgsDto = CopyBeanUtils.fastCopy(item.getCreator(), UserSimpleInfoMgsDto.class);
                userSimpleInfoMgsDto.setUserId(AesUtil.encrypt(item.getCreator().getUserId().toString(), password));
                mgsDto.setCreator(userSimpleInfoMgsDto);
            }
            mgsDtoList.add(mgsDto);
        });

        Page<NftApproveInfoMgsDto> pageResponse = CopyBeanUtils.fastCopy(response.getData(), Page.class);

        APIResponse<Page<NftApproveInfoMgsDto>> result = new APIResponse<>(
                response.getStatus(), response.getType(),
                response.getCode(), response.getErrorData(), pageResponse, response.getParams(), response.getSubData()
        );

        return result;
    }
}
