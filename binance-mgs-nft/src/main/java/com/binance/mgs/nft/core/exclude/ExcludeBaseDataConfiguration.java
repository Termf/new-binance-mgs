package com.binance.mgs.nft.core.exclude;

import com.binance.basedata.api.ErrorReactionApi;
import com.binance.basedata.vo.PageRespVo;
import com.binance.basedata.vo.errorreaction.request.QueryErrorReactionRequest;
import com.binance.basedata.vo.errorreaction.request.UpdateErrorReactionRequest;
import com.binance.basedata.vo.errorreaction.response.ErrorReactionResponse;
import com.binance.master.commons.SearchResult;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.nft.bnbgtwservice.api.data.dto.ErrorReactionItem;
import com.binance.nft.bnbgtwservice.api.data.req.QueryErrorReactionReq;
import com.binance.nft.bnbgtwservice.api.iface.IErrorReactionApi;
import com.binance.platform.mgs.base.helper.BaseHelper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class ExcludeBaseDataConfiguration implements BeanFactoryAware {

    @Resource
    private IErrorReactionApi iErrorReactionApi;
    @Resource
    private BaseHelper baseHelper;
    @Primary
    @Bean
    public ErrorReactionApi mockErrorReactionApi() {
        return new ErrorReactionApi() {
            @Override
            public APIResponse<Integer> create(APIRequest<UpdateErrorReactionRequest> creatRequest) {
                return APIResponse.getOKJsonResult();
            }

            @Override
            public APIResponse<Boolean> update(APIRequest<UpdateErrorReactionRequest> updateRequest) {
                return APIResponse.getOKJsonResult();
            }

            @Override
            public APIResponse<Boolean> delete(Integer id) {
                return APIResponse.getOKJsonResult();
            }

            @Override
            public APIResponse<ErrorReactionResponse> get(Integer id) {
                APIResponse<ErrorReactionItem> resp = iErrorReactionApi.get(id);
                return APIResponse.getOKJsonResult(CopyBeanUtils.fastCopy(resp.getData(), ErrorReactionResponse.class));
            }

            @Override
            public APIResponse<PageRespVo<ErrorReactionResponse>> query(APIRequest<QueryErrorReactionRequest> queryRequest) {
                QueryErrorReactionReq req = CopyBeanUtils.fastCopy(queryRequest.getBody(), QueryErrorReactionReq.class);
                APIResponse<SearchResult<ErrorReactionItem>> resp = iErrorReactionApi.query(APIRequest.instance(req));
                List<ErrorReactionResponse> list = Collections.emptyList();
                if(baseHelper.isOk(resp) && Objects.nonNull(resp.getData()) && CollectionUtils.isNotEmpty(resp.getData().getRows())) {
                    list = resp.getData().getRows().stream()
                            .map(item -> {
                                ErrorReactionResponse res = new  ErrorReactionResponse();
                                res.setId(item.getId());
                                res.setCode(item.getCode());
                                res.setBusiness(item.getBusiness());
                                res.setClientType(item.getClientType());
                                res.setLinkTitle(item.getLinkTitle());
                                res.setLinkUrl(item.getLinkUrl());
                                res.setType(item.getType());
                                return res;
                            })
                            .collect(Collectors.toList());
                }
                PageRespVo<ErrorReactionResponse> pageRespVo = new PageRespVo<>();
                pageRespVo.setTotal(Long.valueOf(list.size()));
                pageRespVo.setRecords(list);
                log.info("mockErrorReactionApi query {} {}", queryRequest.getBody(), resp.getData());
                return APIResponse.getOKJsonResult(pageRespVo);
            }
        };
    }

    @Override
    @SneakyThrows
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

//        if(((DefaultListableBeanFactory)beanFactory).containsBeanDefinition("getSystemMaintenanceAspect")) {
//            ((DefaultListableBeanFactory)beanFactory).removeBeanDefinition("getSystemMaintenanceAspect");
//            ((DefaultListableBeanFactory)beanFactory).registerBeanDefinition("getSystemMaintenanceAspect", new GenericBeanDefinition());
//
//        }
    }
}
