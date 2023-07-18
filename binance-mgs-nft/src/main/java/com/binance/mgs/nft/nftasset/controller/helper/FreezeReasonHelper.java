package com.binance.mgs.nft.nftasset.controller.helper;

import com.binance.master.utils.JsonUtils;
import com.binance.nft.assetservice.api.data.dto.ImageCheckDto;
import com.binance.nft.assetservice.enums.FreezeReasonEnum;
import com.binance.nft.assetservice.enums.HiveResultConvert;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CrowdinHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class FreezeReasonHelper {

    @Resource
    private CrowdinHelper crowdinHelper;

    @Resource
    private BaseHelper baseHelper;

    public String getViewTextByReason(String reasonId) {

        String key = convertKey(reasonId);
        if (StringUtils.isBlank(key)) {
            return null;
        }

        FreezeReasonEnum reason = FreezeReasonEnum.findReasonById(Integer.valueOf(reasonId));
        if (reason == null) {
            return null;
        }
        String message = crowdinHelper.getMessageByKey(key, baseHelper.getLanguage());
        message = StringUtils.equals(message, key) ?
                reason.getDescription()
                : message;
        return message;
    }

    public void enrichMessage(Map<String, Object> params) {

        if (MapUtils.isEmpty(params)) {
            return;
        }
        params.put("extend", null);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            if(Objects.isNull(value)) continue;
            if (value instanceof LinkedHashMap) {
                String displayText = null;
                ImageCheckDto imageCheckDto = JsonUtils.toObj(JsonUtils.toJsonHasNullKey(value), ImageCheckDto.class);
                if(imageCheckDto.getType().equals("logo")) {
                    String message = crowdinHelper.getMessageByKey(HiveResultConvert.selectLogo.getCode(), baseHelper.getLanguage());
                    if(StringUtils.equals(message, HiveResultConvert.selectLogo.getCode())){
                        displayText = HiveResultConvert.selectLogo.getDefaultText() + " '" + imageCheckDto.getContent() + "'";
                    }else{
                        displayText = message + " '" + imageCheckDto.getContent() + "'";
                    }
                } else if (imageCheckDto.getType().equals("content_risk")) {
                    displayText = imageCheckDto.getContent();
                    params.put("extend", imageCheckDto);
                } else {
                    HiveResultConvert convert = HiveResultConvert.findByClazz(imageCheckDto.getContent());
                    if(convert != null){
                        String message = crowdinHelper.getMessageByKey(convert.getCode(), baseHelper.getLanguage());
                        if(!StringUtils.equals(message, convert.getCode())){
                            displayText = message;
                        }else{
                            displayText = convert.getDefaultText();
                        }
                    }
                }
                if(StringUtils.isNotBlank(displayText)){
                    params.put(entry.getKey(), displayText);
                }
            }
        }
    }


    private String convertKey(String key) {

        if (StringUtils.isBlank(key)) {
            return null;
        }
        Integer integer = Integer.valueOf(key);
        if (integer < 10) {
            return "nft-freeze-reason-00" + integer;
        } else {
            return "nft-freeze-reason-0" + integer;
        }
    }
}
