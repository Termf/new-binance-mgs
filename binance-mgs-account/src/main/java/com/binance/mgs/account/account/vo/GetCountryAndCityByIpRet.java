package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@ApiModel("根据ip返回国家，城市和省份subDivision")
@AllArgsConstructor
public class GetCountryAndCityByIpRet {
    private String country;
    private String city;
    private String subDivision;
}
