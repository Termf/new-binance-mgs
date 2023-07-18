package com.binance.mgs.account.account.vo.google;

import lombok.Data;

/**
 * @author Men Huatao (alex.men@binance.com)
 * @date 4/25/22
 */
@Data
public class DecodedAppCheckHeader {
    String kid;
    String typ;
    String alg;
}
