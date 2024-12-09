package com.backend.entity.user;

import com.backend.dto.response.admin.outsourcing.GetOutsourcingsDto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Getter
@Entity
@Table(name = "outsourcing")
public class OutSourcing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company")
    private String company;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "hp")
    private String hp;

    @Column(name = "payment_date")
    private int paymentDate;

    @Column(name = "last_payment_date")
    private String lastPaymentDate;

    @Column(name = "outsourcing_end")
    private String endDate;

    @Column(name = "outsourcing_start")
    private String startDate;

    @Column(name = "outsourcing_size")
    private Integer size;


    public GetOutsourcingsDto toGetOutsourcingDto() {
        return GetOutsourcingsDto.builder()
                .id(id)
                .hp(hp)
                .size(size)
                .name(companyName)
                .paymentDate(paymentDate)
                .start(startDate)
                .end(endDate)
                .build();
    }
}
