package com.binance.mgs.nft.nftasset.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditFileCallbackReq {

    private String id;

    private String projectId;

    private List<RespItemStatus> status;

    @Data
    public static class RespItemStatus{
        private ResponseStatus status;
        private Response response;
    }

    @Data
    public static class ResponseStatus{
        private String code;
        private String message;
    }

    @Data
    public static class Response{
        private String input;
        private List<Classes> output;
    }

    @Data
    public static class Classes{
        private Integer time;
        private List<String> classes;
    }
}
