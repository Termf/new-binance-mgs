package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.assetservice.api.data.vo.NftInfoDto;
import lombok.Data;

@Data
public class NftApproveInfoMgsDto extends NftInfoDto {

    private UserSimpleInfoMgsDto creator;
}