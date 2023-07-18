package com.binance.mgs.nft.access;

import com.binance.master.utils.DateUtils;
import com.binance.master.utils.StringUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.nft.trade.config.TradeConfig;
import com.binance.mgs.nft.utils.RSAUtils;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Aspect
@Slf4j
public class DecodeCheckAop {

    @Resource
    private TradeConfig tradeConfig;


    @Around("@annotation(DecodeCheck)")
    public Object around(ProceedingJoinPoint point) throws Throwable {

        MethodSignature signature = (MethodSignature) point.getSignature();
        DecodeCheck annotation = signature.getMethod().getAnnotation(DecodeCheck.class);
        String activitySignature = WebUtils.getHeader(annotation.headerSignature());
        Object[] args = point.getArgs();
        if(args == null || args.length == 0) {
            return initDecodeCheck();
        }
        AtomicReference<Date> timestamp = new AtomicReference<>();
        String context = generatorParamByTemplate(args[0],annotation.checkParameter(),timestamp);
        String results =RSAUtils.decodeContext(activitySignature, tradeConfig.getActivityPrivateKey());
        boolean flag = DateUtils.getNewUTCTimeMillis() - timestamp.get().getTime() <= tradeConfig.getTimestampPeriod();
        if(timestamp.get() != null && results.equals(context) && flag) {
            return point.proceed();
        }
        return initDecodeCheck();
    }







    private String generatorParamByTemplate(Object arg, String[] checkParameter,AtomicReference<Date> timestamp) {
        StringBuilder sb = new StringBuilder();
        for (String item : checkParameter) {
            try {
                sb.append(item).append("=");
                Object o = FieldUtils.getField(arg.getClass(), item,true).get(arg);
                if(o instanceof Date) {
                    timestamp.set((Date) o);
                    o =  timestamp.get().getTime();
                }else if(o instanceof List) {
                    List o1 = (List) o;
                    o = StringUtils.join(o1,",");
                }
                sb.append(o).append("&");
            } catch (IllegalAccessException e) {
                log.error("generatorParamByTemplate error ",e );
            }
        }
        return sb.substring(0,sb.length() - 1);
    }

    public CommonRet initDecodeCheck() {
        CommonRet commonRet = new CommonRet();
        commonRet.setCode("10000222");
        commonRet.setMessage("Error decode check");
        return commonRet;
    }



    public static void main(String[] args) throws Exception {
        String message = "userId=10898921&code=3UAIl6Ka&timestamp=1655150085426&subActivityCode=590140169946042368,590140169946042368";
        String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgkHKb6iyXDjP/MOOR7wcos9qMeO7OanJBi8zoah0SZNEVjVbmmFfYn8xm+B5YBgtyrFK8+BPtks24e9oJb8DH+MF1o+LReG7I59w2YhMhDYduEX/sKczRl5grNoNAX1mFL0G0Se8ynkpOkBkGes0a6sMvP+B0G1sdktPPx88w9UbSodJhw4v23sq82XAwtbvgZ5f/Dex8JAIOKvKhHkETnwTGtdsRkMIaf8b/iVT7M7T0GQEdXVKA/ABo/vG0qXqqBCqIveKjaFN+vsV/bjMH/1YO78hLCjUppuVsOJoUBDXSMMkIF00wOVwXxw4hSEu8G956HcjZhS6eqg/oCtTVwIDAQAB";
        String publicResults = RSAUtils.encodeByPublicKey(message,RSAUtils.getPublicKey(publicKey));
        System.out.println("publicResults = " + publicResults);
//        String privateKey = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCCQcpvqLJcOM/8w45HvByiz2ox47s5qckGLzOhqHRJk0RWNVuaYV9ifzGb4HlgGC3KsUrz4E+2Szbh72glvwMf4wXWj4tF4bsjn3DZiEyENh24Rf+wpzNGXmCs2g0BfWYUvQbRJ7zKeSk6QGQZ6zRrqwy8/4HQbWx2S08/HzzD1RtKh0mHDi/beyrzZcDC1u+Bnl/8N7HwkAg4q8qEeQROfBMa12xGQwhp/xv+JVPsztPQZAR1dUoD8AGj+8bSpeqoEKoi94qNoU36+xX9uMwf/Vg7vyEsKNSmm5Ww4mhQENdIwyQgXTTA5XBfHDiFIS7wb3nodyNmFLp6qD+gK1NXAgMBAAECggEAAuJIBB7dDBOp7zO5M7djfutOs5oSLB2pOLzUzNB4+qQLEEmQJKPhQ8IDLCtVJJ6EbQdt3GZr/WI+7dOqH6PSAuO43l5BPCPaS9ic3AQbhZXZJJpQJe4dwYIXa9xMC2tmVjE1NG5HzMfP9N02GijN+VBJMOoLSr0ReLEEKSac5s0Jxb6zfwxKhAxJ1uQpJfaOpt2Yo9tnue7OxOQrnj5NTkVTzxdOScyY8UwYAWrxApROLJ+4ludymxt9SVoiqyFJMVhbl3ObB/Uj7zlneoEQjDjM9bLg5lQytHZi3iZo7xyoDqTwcYUc4VApyjgF+YtT8rh7zYpQ3+pgy/O9YLRl0QKBgQDmuHNDiQ6hZ0IWeorlwVFjaR0bbge3Eo4UCVwg7fxVdxmmy1rpqzEul8TQTIuaEVrOINnlVFW2l1PtXKS0qgYDmKmAbzgOjFstF0uAxsnIMLgLKAqeMtxK6xgK8TBbI8eOHg3D+utIwyKJ9rNqDduTie6h9KA2zECnH8C5SwH+HwKBgQCQh2iq0nUixZd9apBlIPppcNpGl7TnwM1uJq1BxiMi1M2AhrBQTf1uN6soUUdvIqzgS4WsVu2OYuOj67LPmQTDfIQbzDyqIVqdCCWqwVXdbB8o/0SMpBDmELamADhMxa0diRllW6/0Yn7HpECL2gTyXzGIuy8+Ehvu88ZMNXGTyQKBgFS2fuPaK/wJTNOyFNO9QmPs0Voj8UM/1dj3gtM4boD25P1AB1Zqm/lOkl4k7NEZ9CxhFYBFkd8j+xXZAUSwdNrXL81PiNaWpFePCRL0alxNvxWhkxx48jez0DUcT7P3FCtTT5yYwdEKjOD5KvESu3+Vkn/2sOjN4CM83mdqagXjAoGAWeI8r/APNT7ZhgAeKSanVaf/t+NleLQpjpWzLrLA60qZO5OIV4kJUeCBK6PQ30cbaKrPSW0OdHz/wdQ18nHhyonHx1nvaIcxyXNlqZpvgjNZ9a87vJPUhqBiVz7PxL8zeKjpCGZLOZt/6T03f0JpzSpyrexr5xhwEt28t2yNZDECgYAHQYqKYdDc+zznGktgutOksUuCUKRvSA+NrG5AqMd6W69fjkgtK6oC+NBML0zJVgAE93MOfmhEPzix25w86g6Pq7rITTyU1tVkXUxiNkF8K/ZiuYacraU+QURgOuOGvXy7EzgWG8+zMuCXjq+WPPb+f+tTwq8q0cI+mi7Z4HcqKg==\n" +
//                "userId=10898921&code=1ioHDhgT&timestamp=1655150085426";
//        System.out.println("privateKey = " + privateKey);
//        String context = RSAUtils.decodeContext(publicResults, privateKey);
//        System.out.println(context);

    }



}
