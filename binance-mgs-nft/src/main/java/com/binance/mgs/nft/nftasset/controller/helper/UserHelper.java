package com.binance.mgs.nft.nftasset.controller.helper;

import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.DateUtils;
import com.binance.mgs.nft.nftasset.vo.UserAccessDto;
import com.binance.mgs.nft.nftasset.vo.UserLimitCountDto;
import com.binance.mgs.nft.nftasset.vo.UserSimpleAccountVo;
import com.binance.nft.assetservice.api.data.vo.MinterInfo;
import com.binance.nft.assetservice.api.data.vo.UserMintVo;
import com.binance.nft.assetservice.api.mintmanager.IMintManagerApi;
import com.binance.nft.bnbgtwservice.api.data.dto.UserSimpleAccountDto;
import com.binance.nft.bnbgtwservice.api.iface.IUserInfoApi;
import com.binance.nft.reconcilication.api.INftWhiteUserQueryApi;
import com.binance.nft.reconcilication.api.NftWhiteListAdminApi;
import com.binance.nft.reconcilication.req.QueryWhiteListRequest;
import com.binance.nft.reconcilication.vo.NFTWhiteListVo;
import com.binance.nft.tradeservice.enums.TradeErrorCode;
import com.binance.nftcore.utils.lambda.check.BaseHelper;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.binance.mgs.nft.core.redis.RedisCommonConfig.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserHelper {


    private final INftWhiteUserQueryApi userQueryApi;

    private final NftWhiteListAdminApi nftWhiteListAdminApi;

    private final IUserInfoApi userInfoApi;

    private final PojoConvertor pojoConvertor;

    private final RedisTemplate redisTemplate;

    private final IMintManagerApi mintManagerApi;

    @Setter
    @Getter
    private volatile List<NFTWhiteListVo> list;

    @PostConstruct
    public void loadCache() {
        Executors.newSingleThreadScheduledExecutor()
                .scheduleWithFixedDelay(
                        this::refreshNFTWhiteListVos, 3, 30, TimeUnit.SECONDS
                );
    }

    //数据量太大了，所以放弃这个方法
    public void refreshNFTWhiteListVos() {
        APIResponse<List<NFTWhiteListVo>> response = userQueryApi.queryWhiteList(APIRequest.instance(new QueryWhiteListRequest()));
        BaseHelper.checkResponse(response);
        setList(response.getData());
    }


    @Value("${nft.pre.white-list.flag.mint:1}")
    private Integer mintFlag;

    public UserAccessDto checkUserWhiteList(Long userId){
        UserAccessDto accessDto = UserAccessDto.builder()
                .visitFlag(Boolean.TRUE)
                .mintFlag(Boolean.FALSE)
                .loginFlag(Boolean.TRUE)
                .build();

        // 关闭白名单
        if(mintFlag.equals(NumberUtils.INTEGER_ZERO)) {
            return accessDto;
        }

        // 开启白名单
        NFTWhiteListVo whiteListVo = getWhiteListVo(userId);
        if (whiteListVo != null  && whiteListVo.getStatus() != null && whiteListVo.getStatus().equals(1)){
            accessDto.setMintFlag(Boolean.TRUE);
        }
        return accessDto;
    }

    public NFTWhiteListVo getWhiteListVo(Long userId) {
        List<NFTWhiteListVo> collect = list.stream().filter(item -> item.getUserId().equals(userId)).collect(Collectors.toList());
        return CollectionUtils.isEmpty(collect) ? null : collect.get(0);
    }

    public Integer checkMintCountV2(Long userId) {
        APIResponse<MinterInfo> minterInfoAPIResponse = mintManagerApi.checkUserMintable(APIRequest.instance(String.valueOf(userId)));
        if(minterInfoAPIResponse.getData()!=null
                && minterInfoAPIResponse.getData().getStatus()!=null
                && "Available".equalsIgnoreCase(minterInfoAPIResponse.getData().getStatus())) {
            return minterInfoAPIResponse.getData().getTotalMintCount();
        }else{
            throw new BusinessException(TradeErrorCode.MINT_OVER_LIMIT);
        }
    }

    public Integer checkMintCount(Long userId) {
        NFTWhiteListVo whiteListVo = getWhiteListVo(userId);
        if(whiteListVo == null || whiteListVo.getRankVo() == null) {
            throw new BusinessException(TradeErrorCode.MINT_OVER_LIMIT);
        }
        if(whiteListVo.getRankVo().getLimitMint() == -1 ) {
            return whiteListVo.getRankVo().getLimitMint();
        }
        if(doCheckMintCount(userId,whiteListVo)) {
            return whiteListVo.getRankVo().getLimitMint();
        }
        throw new BusinessException(TradeErrorCode.MINT_OVER_LIMIT);
    }

    public Integer checkOnSaleCount(Long userId, List<Long> nftIds) {
        APIResponse<UserMintVo> minterInfoAPIResponse = mintManagerApi.getUserMinManagerInfo(APIRequest.instance(userId));
        Integer listCount = 0;
        if(minterInfoAPIResponse.getData() != null) {
            listCount = minterInfoAPIResponse.getData().getTotalOnSaleCount();
        }else{
            throw new BusinessException(TradeErrorCode.ONSALE_OVER_LIMIT);
        }
        if(listCount == -1 ) {
            return listCount;
        }
        if(doCheckOnSaleCount(userId, listCount, nftIds.get(0))) {
            return listCount;
        }
        throw new BusinessException(TradeErrorCode.ONSALE_OVER_LIMIT);
    }
    public boolean doCheckOnSaleCount(Long userId, Integer listCount, Long nftId) {
        Integer count  = incriAndGet(userId,NFT_WHITE_LIMIT_ONSALE_COUNT, listCount);
        return listCount > count;
    }

    public boolean doCheckMintCount(Long userId,NFTWhiteListVo whiteListVo) {
        Integer count  = incriAndGet(userId,NFT_WHITE_LIMIT_MINT_COUNT,whiteListVo.getRankVo().getLimitMint());
       return whiteListVo.getRankVo().getLimitMint() > count;
    }

    private Integer incriAndGet(Long userId, String key, Integer limit) {
        return (Integer)redisTemplate.execute(new RedisCallback<Integer>() {
            @Override
            public Integer doInRedis(RedisConnection connection) throws DataAccessException {
                String format = generatorKey(userId,key);
                byte[] bytes = connection.get(format.getBytes(StandardCharsets.UTF_8));
                if(bytes != null) {
                    int count = Integer.parseInt(new String(bytes));
                    if(count >= limit ) {
                         return count;
                     }
                }
                Long res = connection.incr(format.getBytes(StandardCharsets.UTF_8));
                if(bytes == null) {
                    connection.expire(format.getBytes(StandardCharsets.UTF_8), getExpireTime());
                }
                return 0;
            }
        });
    }

    public void decrMintCount(Long userId,Integer limit) {
        if(limit == -1) {
            return;
        }
        decrAndGet(userId,NFT_WHITE_LIMIT_MINT_COUNT,limit);
    }

    public void decrOnSaleCount(Long userId,Integer limit) {
        if(limit == -1) {
            return;
        }
        decrAndGet(userId,NFT_WHITE_LIMIT_ONSALE_COUNT,limit);
    }

    private Integer decrAndGet(Long userId, String key, Integer limit) {
        return (Integer)redisTemplate.execute(new RedisCallback<Integer>() {
            @Override
            public Integer doInRedis(RedisConnection connection) throws DataAccessException {
                String format = generatorKey(userId,key);
                Long res = connection.decr(format.getBytes(StandardCharsets.UTF_8));
                connection.expire(format.getBytes(StandardCharsets.UTF_8), getExpireTime());
                return res.intValue();
            }
        });
    }

    private UserLimitCountDto getLimit(Long userId, NFTWhiteListVo whiteListVo) {
        return (UserLimitCountDto)redisTemplate.execute(new RedisCallback<UserLimitCountDto>() {
            @Override
            public UserLimitCountDto doInRedis(RedisConnection connection) throws DataAccessException {
                String format = generatorKey( userId,NFT_WHITE_LIMIT_MINT_COUNT);
                byte[] bytes = connection.get(format.getBytes(StandardCharsets.UTF_8));
                int mintCount = bytes == null ? 0 : Integer.parseInt(new String(bytes));
                String onsaleFormate = generatorKey(userId, NFT_WHITE_LIMIT_ONSALE_COUNT);
                byte[] onsaleBytes = connection.get(onsaleFormate.getBytes(StandardCharsets.UTF_8));
                int onsaleCount = onsaleBytes == null ? 0 : Integer.parseInt(new String(onsaleBytes));
                return UserLimitCountDto.builder().limitMintCount(whiteListVo.getRankVo().getLimitMint())
                        .limitOnsaleCount(whiteListVo.getRankVo().getLimitOnsale())
                        .remainMintCount(whiteListVo.getRankVo().getLimitMint() - mintCount)
                        .remainOnSaleCount(whiteListVo.getRankVo().getLimitOnsale() - onsaleCount).build();
            }
        });
    }

    private long getExpireTime() {
        Date newDate = DateUtils.getNewUTCDate();
        Date dateEnd = DateUtils.getDateEnd(newDate);
        return ChronoUnit.SECONDS.between(newDate.toInstant(), dateEnd.toInstant());
    }

    public UserLimitCountDto getLimitCount(Long userId) {
        NFTWhiteListVo whiteListVo = getWhiteListVo(userId);
        if(whiteListVo == null || whiteListVo.getRankVo() == null){
            return UserLimitCountDto.builder().build();
        }
        return getLimit(userId,whiteListVo);
    }

    public void cacheNftIds(Long userId, List<Long> nftIds, Integer onSaleCount) {
        if(onSaleCount == -1) {
            return;
        }
        pushOnSaleNfts(userId,nftIds.get(0));
    }

    private void pushOnSaleNfts(Long userId, Long nftId) {
        redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                String format = generatorKey(userId,NFT_WHITE_LIMIT_ONSALE_NFTLIST);
                byte[] bytes = format.getBytes(StandardCharsets.UTF_8);
                connection.hSet(bytes, nftId.toString().getBytes(StandardCharsets.UTF_8),"0".getBytes(StandardCharsets.UTF_8));
                connection.expire(format.getBytes(StandardCharsets.UTF_8),getExpireTime());
                return null;
            }
        });
    }

    private  String generatorKey(Long userId, String template) {
        long time = DateUtils.getDateEnd(DateUtils.getNewUTCDate()).getTime();
        return String.format(template,userId,time);
    }

    public void checkNftIdExist(Long userId, Long nftId) {
        if(nftId == null) {
            return;
        }
        redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                String format = generatorKey(userId,NFT_WHITE_LIMIT_ONSALE_NFTLIST);
                byte[] bytes = format.getBytes(StandardCharsets.UTF_8);
                byte[] hkeyBytes = nftId.toString().getBytes(StandardCharsets.UTF_8);
                Boolean exist = connection.hExists(bytes, hkeyBytes);
                if(exist) {
                    Long result = connection.hDel(bytes, hkeyBytes);
                    String key = generatorKey(userId, NFT_WHITE_LIMIT_ONSALE_COUNT);
                    connection.decr(key.getBytes(StandardCharsets.UTF_8));
                }
                return null;
            }
        });
    }

    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_BIG)
    public UserSimpleAccountVo fetchUserSimpleAccount(Long userId){

        final APIResponse<UserSimpleAccountDto> apiResponse = userInfoApi.getUserAccountSimple(userId);
        BaseHelper.checkResponse(apiResponse);

        return pojoConvertor.copyAccountSimple(apiResponse.getData());
    }
}
