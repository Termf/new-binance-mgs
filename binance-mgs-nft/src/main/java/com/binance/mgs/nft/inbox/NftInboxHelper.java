package com.binance.mgs.nft.inbox;

import com.alibaba.ttl.threadpool.TtlExecutors;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.binance.master.commons.SearchResult;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.nft.notificationservice.api.INotificationInboxApi;
import com.binance.nft.notificationservice.api.data.bo.BizIdModel;
import com.binance.nft.notificationservice.api.request.InboxBizRequest;
import com.binance.nft.notificationservice.api.response.InboxUnreadResponse;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
@Service
public class NftInboxHelper {
    private ExecutorService executorService;

    @Value("${nft.mgs.inbox.unreadCoreSize:5}")
    private int unreadCoreSize;
    @Value("${nft.mgs.inbox.maxWaitMs:500}")
    private int maxWaitMs;

    @Autowired
    private INotificationInboxApi notificationInboxApi;
    @Autowired
    private BaseHelper baseHelper;

    @PostConstruct
    private void init() {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(unreadCoreSize, unreadCoreSize,
                1L, TimeUnit.MINUTES, new LinkedBlockingQueue<>(100),
                new ThreadFactoryBuilder().setNameFormat("nft-inbox-unread-%d").build(),
                new ThreadPoolExecutor.DiscardPolicy());
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        executorService = TtlExecutors.getTtlExecutorService(threadPoolExecutor);
    }

    private List<Long> getInboxBizId(Long userId, List<BizIdModel> bizIdList) {
        InboxBizRequest inboxBizRequest = new InboxBizRequest();
        inboxBizRequest.setUserId(userId);
        inboxBizRequest.setBizIdList(bizIdList);
        APIResponse<InboxUnreadResponse> response = notificationInboxApi.unreadByBizId(APIRequest.instance(inboxBizRequest));
        baseHelper.checkResponse(response);
        if (response.getData() != null && response.getData().getUnreadBizIdList() != null) {
            return response.getData().getUnreadBizIdList();
        } else {
            return Collections.emptyList();
        }
    }

    public <T> void appendHistoryFlag(Long userId, List<T> voList, List<BizIdModel> bizIdList, Function<T, Long> bizIdFun, BiConsumer<T, Boolean> unreadSetter) {
        Future future = executorService.submit(() -> addUnreadFlagInternal(userId, voList, bizIdList, bizIdFun, unreadSetter));
        try {
            future.get(maxWaitMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("appendMintInboxFlag inbox unread bizId error historyType={}", bizIdList.stream().map(b->b.getHistoryType()).collect(Collectors.toList()), e);
        }
    }

    public <T, R> Page<R> pageResultWithFlag(Page<T> originResult, Class<R> clazz,Function<R, Long> bizIdFun, List<BizIdModel> bizIdList, BiConsumer<R, Boolean> unreadSetter) {
        Page<R> retResult = buildPageResult(originResult, clazz);
        if (CollectionUtils.isEmpty(retResult.getRecords())) {
            return retResult;
        }
        addUnreadHistoryFlag(retResult.getRecords(), bizIdFun, bizIdList, unreadSetter);
        return retResult;
    }

    public <T, R> SearchResult<R> searchResultWithFlag(SearchResult<T> originResult, Class<R> clazz, Function<R, Long> bizIdFun, List<BizIdModel> bizIdList, BiConsumer<R, Boolean> unreadSetter) {
        SearchResult<R> retResult = buildSearchResult(originResult, clazz);
        if (CollectionUtils.isEmpty(retResult.getRows())) {
            return retResult;
        }
        addUnreadHistoryFlag(retResult.getRows(), bizIdFun, bizIdList, unreadSetter);
        return retResult;
    }

    public <T> void addUnreadHistoryFlag(List<T> voList, Function<T, Long> bizIdFun, List<BizIdModel> bizIdList, BiConsumer<T, Boolean> unreadSetter) {
        Long userId = baseHelper.getUserId();
        Future future = executorService.submit(() -> addUnreadFlagInternal(userId, voList, bizIdList, bizIdFun, unreadSetter));
        try {
            future.get(maxWaitMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("appendMintInboxFlag inbox unread bizId error historyType={}", bizIdList.stream().map(b->b.getHistoryType()).collect(Collectors.toList()), e);
        }
    }

    public static <T, R> Page<R> buildPageResult(Page<T> originResult, Class<R> clazz) {
        Page<R> retResult = new Page<>();
        if (originResult != null && CollectionUtils.isNotEmpty(originResult.getRecords())) {
            retResult.setTotal(originResult.getTotal());
            List<R> rows = new ArrayList<>(originResult.getRecords().size());
            for (T item : originResult.getRecords()) {
                rows.add(CopyBeanUtils.fastCopy(item, clazz));
            }
            retResult.setRecords(rows);
            retResult.setCurrent(originResult.getCurrent());
            retResult.setSize(originResult.getSize());
        } else {
            retResult.setTotal(0);
            retResult.setRecords(Collections.emptyList());
        }
        return retResult;
    }

    public static <T, R> SearchResult<R> buildSearchResult(SearchResult<T> searchResult, Class<R> clazz) {
        SearchResult<R> retResult = new SearchResult<>();
        if (searchResult != null && CollectionUtils.isNotEmpty(searchResult.getRows())) {
            retResult.setTotal(searchResult.getTotal());
            List<R> rows = new ArrayList<>(searchResult.getRows().size());
            for (T item : searchResult.getRows()) {
                rows.add(CopyBeanUtils.fastCopy(item, clazz));
            }
            retResult.setRows(rows);
        } else {
            retResult.setTotal(0);
            retResult.setRows(Collections.emptyList());
        }
        return retResult;
    }

    private <T> void addUnreadFlagInternal(Long userId, List<T> voList, List<BizIdModel> bizIdList, Function<T, Long> bizIdFun, BiConsumer<T, Boolean> unreadSetter) {
        try {
            List<Long> unreadList = getInboxBizId(userId, bizIdList);
            if (CollectionUtils.isEmpty(unreadList)) {
                return;
            }
            Set<Long> bizIdSet = new HashSet<>(unreadList);
            voList.forEach(vo -> {
                if (bizIdSet.contains(bizIdFun.apply(vo))) {
                    unreadSetter.accept(vo, true);
                }
            });
        } catch (Exception e) {
            log.error("fetch inbox unread bizId error bizType={} userId={}", bizIdList.stream().map(b->b.getHistoryType()).collect(Collectors.toList()), userId, e);
        }
    }
}
