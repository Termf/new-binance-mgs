package com.binance.mgs.nft.trade.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@Data
public class TradeConfig {

    @Value("${nft.trade.createorder.binancepay.scret.key:o0vdiuseh7zz7yzkktapopftca6unohcpmetyurk2dbjztymrpqmql2a1zfvfo34}")
    private String signature;

    @Value("${nft.trade.createorder.binancepay.certsn.key:e9cc62b3e7b84ee1ffcd5b0d098d4d285c67349302cea386184af18cfd6eb4d1}")
    private  String certSn;

    @Value("${nft.trade.asset.account.limit.amount:100000}")
    private BigDecimal maxAccountLimitAmount;

    @Value("${nft.trade.createorder.binancepay.merchantId.key:1000002444324}")
    private String merchantId;


    @Value("${nft.trade.actvitiy.private.key:MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCCQcpvqLJcOM/8w45HvByiz2ox47s5qckGLzOhqHRJk0RWNVuaYV9ifzGb4HlgGC3KsUrz4E+2Szbh72glvwMf4wXWj4tF4bsjn3DZiEyENh24Rf+wpzNGXmCs2g0BfWYUvQbRJ7zKeSk6QGQZ6zRrqwy8/4HQbWx2S08/HzzD1RtKh0mHDi/beyrzZcDC1u+Bnl/8N7HwkAg4q8qEeQROfBMa12xGQwhp/xv+JVPsztPQZAR1dUoD8AGj+8bSpeqoEKoi94qNoU36+xX9uMwf/Vg7vyEsKNSmm5Ww4mhQENdIwyQgXTTA5XBfHDiFIS7wb3nodyNmFLp6qD+gK1NXAgMBAAECggEAAuJIBB7dDBOp7zO5M7djfutOs5oSLB2pOLzUzNB4+qQLEEmQJKPhQ8IDLCtVJJ6EbQdt3GZr/WI+7dOqH6PSAuO43l5BPCPaS9ic3AQbhZXZJJpQJe4dwYIXa9xMC2tmVjE1NG5HzMfP9N02GijN+VBJMOoLSr0ReLEEKSac5s0Jxb6zfwxKhAxJ1uQpJfaOpt2Yo9tnue7OxOQrnj5NTkVTzxdOScyY8UwYAWrxApROLJ+4ludymxt9SVoiqyFJMVhbl3ObB/Uj7zlneoEQjDjM9bLg5lQytHZi3iZo7xyoDqTwcYUc4VApyjgF+YtT8rh7zYpQ3+pgy/O9YLRl0QKBgQDmuHNDiQ6hZ0IWeorlwVFjaR0bbge3Eo4UCVwg7fxVdxmmy1rpqzEul8TQTIuaEVrOINnlVFW2l1PtXKS0qgYDmKmAbzgOjFstF0uAxsnIMLgLKAqeMtxK6xgK8TBbI8eOHg3D+utIwyKJ9rNqDduTie6h9KA2zECnH8C5SwH+HwKBgQCQh2iq0nUixZd9apBlIPppcNpGl7TnwM1uJq1BxiMi1M2AhrBQTf1uN6soUUdvIqzgS4WsVu2OYuOj67LPmQTDfIQbzDyqIVqdCCWqwVXdbB8o/0SMpBDmELamADhMxa0diRllW6/0Yn7HpECL2gTyXzGIuy8+Ehvu88ZMNXGTyQKBgFS2fuPaK/wJTNOyFNO9QmPs0Voj8UM/1dj3gtM4boD25P1AB1Zqm/lOkl4k7NEZ9CxhFYBFkd8j+xXZAUSwdNrXL81PiNaWpFePCRL0alxNvxWhkxx48jez0DUcT7P3FCtTT5yYwdEKjOD5KvESu3+Vkn/2sOjN4CM83mdqagXjAoGAWeI8r/APNT7ZhgAeKSanVaf/t+NleLQpjpWzLrLA60qZO5OIV4kJUeCBK6PQ30cbaKrPSW0OdHz/wdQ18nHhyonHx1nvaIcxyXNlqZpvgjNZ9a87vJPUhqBiVz7PxL8zeKjpCGZLOZt/6T03f0JpzSpyrexr5xhwEt28t2yNZDECgYAHQYqKYdDc+zznGktgutOksUuCUKRvSA+NrG5AqMd6W69fjkgtK6oC+NBML0zJVgAE93MOfmhEPzix25w86g6Pq7rITTyU1tVkXUxiNkF8K/ZiuYacraU+QURgOuOGvXy7EzgWG8+zMuCXjq+WPPb+f+tTwq8q0cI+mi7Z4HcqKg==}")
    private String activityPrivateKey;


    @Value("${nft.trade.actvitiy.private.limit.timestamp:3000}")
    private Long timestampPeriod;



}
