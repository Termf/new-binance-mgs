package com.binance.mgs.nft.activity.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author: felix
 * @date: 16.3.22
 * @description:
 */
@Data
public class SubActivityRequest implements Serializable {

    private static final long serialVersionUID = 2426310948837953118L;
    @NotNull
    private Long subActivityCode;
}
