package com.binance.mgs.nft.payment.request;

import com.binance.nft.paymentservice.api.request.EncryptionRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@Data
public class PaymentEncryptionRequest extends EncryptionRequest implements Serializable {

    private Long productId;

    private List<Long> nftIds = new ArrayList<>();
}
