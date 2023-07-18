package com.binance.mgs.account.authcenter.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DomainTemplate {
    private DomainsConfig domains;

    @Getter
    @Setter
    public static class DomainsConfig {
        private List<ApiDomainInfo> apiAllDomain;
    }

    @Getter
    @Setter
    public static class ApiDomainInfo {
        private String cdnDomain;
        private String cdnPubDomain;
        private String apiDomain;
        private String name;
        private String key;
        private String webviewDomain;
        private String webDomain;
        private String networkPolicy;
    }
}
