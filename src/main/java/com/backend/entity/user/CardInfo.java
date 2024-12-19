package com.backend.entity.user;

import com.backend.dto.response.UserDto;
import com.backend.dto.response.user.RespCardInfoDTO;
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
    private int status; // 만료 여부?

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "card_active_status")
    private int activeStatus; // 자주 쓰는 카드

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

    @Column(name = "card_company")
    private String cardCompany;

    @Column(name = "global_payment")
    private Integer globalPayment; //1 마스터카드 2 비자 3 아메리칸어쩌고

    public void updateUserid(Long id) {
        this.userId = id;
    }

    public RespCardInfoDTO toDto() {
        return RespCardInfoDTO
                .builder()
                .cardId(this.cardId)
                .status(this.status)
                .userId(this.userId)
                .activeStatus(this.activeStatus)
                .paymentCardNo(this.paymentCardNo)
                .paymentCardNick(this.paymentCardNick)
                .paymentCardExpiration(this.paymentCardExpiration)
                .paymentCardCvc(this.paymentCardCvc)
                .autoPayment(this.autoPayment)
                .cardCompany(this.cardCompany)
                .globalPayment(this.globalPayment)
                .build();
    }
}
