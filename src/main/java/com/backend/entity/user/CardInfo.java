package com.backend.entity.user;

import jakarta.persistence.*;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Entity
public class CardInfo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long cardId;

    @Column(name = "card_status")
    private int status;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "card_active_status")
    private int activeStatus; // 기본등록카드

    @Column(name = "card_no")
    private String paymentCardNo; // 카드번호

    @Column(name = "card_nick")
    private String paymentCardNick; // 카드별명

    @Column(name = "card_expiration")
    private String paymentCardExpiration; // 카드만료 년월

    @Column(name = "card_cvc")
    private String paymentCardCvc; // cvc

    @Column(name = "auto_payment")
    private int autoPayment; // 자동결제여부
}
