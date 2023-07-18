package com.binance.mgs.account.util;



import java.util.regex.Pattern;

public class RegexUtils {

    public final static String PASSWORD_PATTERN = "^[0-9a-f]{128}$";


    /**
     *  正则：手机号（简单）, 1字头＋10位数字即可.(实际上只是中国的可以这么搞，国外的么就再说了)
     * @return
     */
    public static boolean validateMobilePhone(String in) {
        Pattern pattern = Pattern.compile("^[1]\\d{10}$");
        return pattern.matcher(in).matches();
    }

    /**
     *  正则：手机号（简单）, 1字头＋10位数字即可.(实际上只是中国的可以这么搞，国外的么就再说了)
     * @return
     */
    public static boolean validateOnlyChinaMobilePhone(String mobileCode,String mobile) {
        boolean isChinaPhoneNumber=mobileCode.equalsIgnoreCase("cn")||mobileCode.equalsIgnoreCase("86");
        if(!isChinaPhoneNumber){
            return true;
        }
        Pattern pattern = Pattern.compile("^[1]\\d{10}$");
        return pattern.matcher(mobile).matches();
    }


    /**
     * check true代表通过，false代表失败
     * @param password
     * @return
     * @throws Exception
     */
    public static boolean validateSafePassword(String password)throws Exception {
        // check password length
        Pattern pattern = Pattern.compile(PASSWORD_PATTERN);
        return pattern.matcher(password).matches();
    }




}
