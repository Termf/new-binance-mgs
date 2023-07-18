package com.binance.mgs.account.account.vo;

import com.binance.account.vo.user.response.SnapshotShareConfigRes;
import com.google.common.collect.Lists;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by yangyang on 2019/10/21.
 */
@Data
public class SnapshotUploadResponse implements Serializable {

    private String uploadUrl;

    private String agentCode;
    /**
     * 本次上传的唯一key
     */
    private String fileLink;

    /**
     * 国内分享link
     */
    private String cnfileLink;

    private List<SnapshotShareConfigRes> configs = Lists.newArrayList();
}
