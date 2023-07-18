package com.binance.mgs.nft.market.helper;

import com.binance.master.models.APIResponse;
import com.binance.nft.assetservice.api.data.vo.function.NftLayerConfigVo;
import com.binance.nft.assetservice.api.function.ILayerInfoApi;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author: felix
 * @date: 23.5.22
 * @description:
 */
@Slf4j
@Component
public class NftLayerInfoHelper extends BaseHelper{



}
