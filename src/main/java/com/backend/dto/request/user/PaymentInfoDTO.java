package com.backend.dto.request.user;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentInfoDTO {
    private int activeStatus; // 기본등록카드
    private String paymentCardNo; // 카드번호
    private String paymentCardNick; // 카드별명
    private String paymentCardExpiration; // 카드만료 년월
    private String paymentCardCvc; // cvc
    private int autoPayment; // 자동결제여부
    private String cardCompany;
}
