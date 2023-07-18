package com.binance.mgs.account.util;


import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;


public class AnnualBillUtil {

    private static final Map<String,AnnualBillUtil> map = new HashMap<>(9);

    private String key;

    private String enKeyWord;

    private String cnKeyWord;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getEnKeyWord() {
        return enKeyWord;
    }

    public void setEnKeyWord(String enKeyWord) {
        this.enKeyWord = enKeyWord;
    }

    public String getCnKeyWord() {
        return cnKeyWord;
    }

    public void setCnKeyWord(String cnKeyWord) {
        this.cnKeyWord = cnKeyWord;
    }

    static {
        AnnualBillUtil annualBillUtil = new AnnualBillUtil();
        annualBillUtil.setKey("1");
        annualBillUtil.setCnKeyWord("合约精英");
        annualBillUtil.setEnKeyWord("Futures Elite");
        map.put("1",annualBillUtil);

        AnnualBillUtil annualBillUtil2 = new AnnualBillUtil();
        annualBillUtil2.setKey("2");
        annualBillUtil2.setCnKeyWord("合约达人");
        annualBillUtil2.setEnKeyWord("Futures Talent");
        map.put("2",annualBillUtil2);

        AnnualBillUtil annualBillUtil3 = new AnnualBillUtil();
        annualBillUtil3.setKey("3");
        annualBillUtil3.setCnKeyWord("投资大神");
        annualBillUtil3.setEnKeyWord("Master Trader");
        map.put("3",annualBillUtil3);

        AnnualBillUtil annualBillUtil4 = new AnnualBillUtil();
        annualBillUtil4.setKey("4");
        annualBillUtil4.setCnKeyWord("高阶玩家");
        annualBillUtil4.setEnKeyWord("High-level trader");
        map.put("4",annualBillUtil4);

        AnnualBillUtil annualBillUtil5 = new AnnualBillUtil();
        annualBillUtil5.setKey("5");
        annualBillUtil5.setCnKeyWord("硬核玩家");
        annualBillUtil5.setEnKeyWord("Hardcore Trader");
        map.put("5",annualBillUtil5);

        AnnualBillUtil annualBillUtil6 = new AnnualBillUtil();
        annualBillUtil6.setKey("6");
        annualBillUtil6.setCnKeyWord("潜力股");
        annualBillUtil6.setEnKeyWord("Potential Trader");
        map.put("6",annualBillUtil6);

        AnnualBillUtil annualBillUtil7 = new AnnualBillUtil();
        annualBillUtil7.setKey("7");
        annualBillUtil7.setCnKeyWord("未来星");
        annualBillUtil7.setEnKeyWord("Future Star");
        map.put("7",annualBillUtil7);


        AnnualBillUtil annualBillUtil8 = new AnnualBillUtil();
        annualBillUtil8.setKey("8");
        annualBillUtil8.setCnKeyWord("穿越牛熊");
        annualBillUtil8.setEnKeyWord("Crypto Veteran");
        map.put("8",annualBillUtil8);

        AnnualBillUtil annualBillUtil9 = new AnnualBillUtil();
        annualBillUtil9.setKey("9");
        annualBillUtil9.setCnKeyWord("币安萌新");
        annualBillUtil9.setEnKeyWord("Blockchain Beginner");
        map.put("9",annualBillUtil9);
    }

}
