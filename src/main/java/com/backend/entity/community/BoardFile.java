package com.backend.entity.community;
/*
    날짜 : 2024/12/03
    이름 : 박서홍
    내용 : File Entity 작성
 */
import jakarta.persistence.*;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Entity
public class BoardFile extends BaseTimeEntity{
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "file_no")
    private int fileNo;

    @Column(name = "article_no")
    private int articleNo;

    @Column(name = "o_name")
    private String oName;

    @Column(name = "s_name")
    private String sName;

    @Column(name = "download_count")
    private int downloadCount;





}
