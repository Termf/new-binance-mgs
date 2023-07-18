package com.binance.mgs.nft.fantoken.helper;

import com.alibaba.fastjson.JSONObject;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.LanguageUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class FanTokenBTSHelper {

    @Value("${fantoken.bts.server:https://static.devfdg.net/api/i18n/-/web/cms/sp/17753ba17}")
    private String btsUrl;
    @Value("${fantoken.bts.project:fantoken}")
    private String project;
    @Value("${fantoken.bts.lang.mapping:{\"cn\":\"zh-CN\",\"de\":\"de\",\"en\":\"en\",\"au\":\"en-AU\",\"es-es\":\"es\",\"es-la\":\"es-LA\",\"fr\":\"fr\",\"in\":\"id\",\"it\":\"it\",\"kr\":\"ko\",\"nl\":\"nl\",\"pl\":\"pl\",\"pt\":\"pt-BR\",\"br\":\"pt-BR\",\"pt-pt\":\"pt-PT\",\"ru\":\"ru\",\"tr\":\"tr\",\"tw\":\"zh-TW\",\"ua\":\"uk-UA\",\"uk-ua\":\"uk-UA\",\"vn\":\"vi\",\"ph\":\"fil\",\"ja\":\"ja\",\"th\":\"th\",\"ar\":\"ar\",\"ro\":\"ro\",\"cs\":\"cs\",\"iw\":\"he\",\"bg\":\"bg\",\"lv\":\"lv\",\"bn\":\"bn\",\"sv\":\"sv\",\"zh-tc\":\"zh-TW\",\"es-mx\":\"es-LA\",\"es-ar\":\"es-LA\",\"fi\":\"fi\",\"sk\":\"sk\",\"sl\":\"sl\",\"el\":\"el\",\"hi\":\"hi\",\"ur\":\"ur\"}}")
    private String langMapping;

    private static final String SOURCE_LANG = "source";

    // HTTP GET 超时时间为5秒
    private static final int HTTP_REQUEST_TIME_OUT = 5_000;
    private Map<String, String> lang2SmartlingLang;// cn:zh-CN
    /**
     * lang => [code, translate] lang: lang2SmartlingLang的value
     */
    private final Map<String, Map<String, String>> localCache = Maps.newConcurrentMap();
    private ScheduledFuture<?> timer;

    @PostConstruct
    public void init() {
        lang2SmartlingLang = buildLang2SmartlingLangMap();
        // 维持 SmartlingHelper 相同刷新频率
        timer = Executors.newSingleThreadScheduledExecutor()
                .scheduleWithFixedDelay(this::refreshLocalCache, 0, 1L, TimeUnit.HOURS);
    }

    public String getMessageByKey(String key, String lang) {
        return StringUtils.defaultIfBlank(this.getTransCacheable(key, lang), key);
    }

    // binance-mgs-common 里取lang -> smartling lang 的配置
    private Map<String,String> buildLang2SmartlingLangMap() {
        //String v = ConfigService.getConfig("ops.binance-mgs-common").getProperty("crowdin.lang.mapping", null);
        String v = langMapping;
        Map<String, String> mapping = JSONObject.parseObject(v, Map.class);
        if (mapping == null) {
            log.error("apollo key {} not config!", "crowdin.lang.mapping");
            mapping = Collections.emptyMap();
        }
        return mapping;
    }

    private String getTransCacheable(String code, String lang) {
        lang = lang.toLowerCase();
        lang = LanguageUtils.languageMapping(lang);
        String trans = null;
        try {
            if (!lang2SmartlingLang.containsValue(lang)) // 如果不是smartling的langCode，则转成smartling的。
                lang = lang2SmartlingLang.getOrDefault(lang, lang);
            // 如果smartling上没有对应lang的翻译，则取sourceString的原文(一般是英文)
            Map<String, String> codeMap = localCache.getOrDefault(lang, localCache.get(SOURCE_LANG));
            if (codeMap != null) {
                trans = codeMap.get(code);
            }
        } catch (Exception e) {
            // ignore
        }
        return trans;
    }

    private void refreshLocalCache() {
        long start = System.currentTimeMillis();
        Multimap<String, String> updateMap = Multimaps.synchronizedSetMultimap(HashMultimap.create()); // lang :
        // Set<code>
        lang2SmartlingLang.entrySet().parallelStream().forEach(en -> {
            String langK = en.getKey(); // zh tw en
            String langV = en.getValue();// zh-CN zh-TW en
            try {
                // https://bin.bnbstatic.com/api/i18n/-/web/cms/sp/17753ba17/zh-CN/fantoken
                Map<String, String> trans =
                        doGet(btsUrl+"/"+langV+"/"+project,
                                Collections.singletonMap("Referer", "https://www.binance.com/"),
                                new TypeReference<Map<String, String>>() {
                                });
                Optional.ofNullable(trans).ifPresent(t -> {
                    localCache.put(langV, t);
                    updateMap.putAll(langV, t.keySet());
                    if (langK.equals("en")) {
                        localCache.put(SOURCE_LANG, t);
                        updateMap.putAll(SOURCE_LANG, t.keySet());
                    }
                });
            } catch (Exception e) {
                log.error("fetch lang {} from cdn err! {}", langK, e.getMessage());
            }
        });
        if (log.isDebugEnabled()) {
            log.debug("cdn fetching finish. langs: {}, keys: {}", updateMap.keySet(), updateMap.get(SOURCE_LANG));
        }
        log.info("cdn fetching finish. cost: {}", System.currentTimeMillis() - start);
    }

    private  <T> T doGet(String url, Map<String, String> headers, TypeReference<T> responseType) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL path = new URL(url);
            conn = (HttpURLConnection) path.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(HTTP_REQUEST_TIME_OUT);
            if (headers != null) {
                headers.forEach(conn::setRequestProperty);
            }
            conn.connect();
            try (InputStream inputStream = conn.getInputStream();
                 ByteArrayOutputStream result = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                String response = result.toString("UTF-8");
                return JsonUtils.parse(response, responseType);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
