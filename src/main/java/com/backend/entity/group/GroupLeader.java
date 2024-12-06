package com.backend.entity.group;

import com.backend.dto.response.user.GetUsersAllDto;
import com.backend.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.Optional;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@ToString
@Entity
@Table(name = "group_leader")
public class GroupLeader {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_leader_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    @ToString.Exclude
    private Group group;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public void patchLeader(User user) {
        this.user = user;
    }


    public void putLeader(User user) {
        this.user = user;
    }
}
