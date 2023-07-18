package com.binance.mgs.nft.nftasset.response;

import lombok.*;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class PreMintCheckResponse  implements Serializable {
    private String tplCode;
    private Map<String,Object> extendInfo;

    @Getter
    @AllArgsConstructor
    public enum TplCodeEnum {

        NOT_TEST_USER("1001", "NOT_TEST_USER"),
        SWITCH_OFF("1002", "SWITCH_OFF"),
        FOLLOWER_LIMIT("1003", "FOLLOWER_LIMIT"),
        USER_BANED("1004", "USER_BANED"),
        MINT_COUNT_LIMIT("1005", "MINT_COUNT_LIMIT"),
        USER_NOT_AGREED("1006", "USER_NOT_AGREED"),
        ;

        private String code;
        private String desc;

        public static String getTplDesc(String code){
            for(TplCodeEnum tplCode : TplCodeEnum.values()){
                if(Objects.equals(tplCode.code, code)){
                    return tplCode.getDesc();
                }

            }
            return null;
        }
    }

}
