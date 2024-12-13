package com.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String email;
    private String password;
    private String name;
    private String phone;
    private String address;
    private String profileImg;
    
    public String getProfileImg() {
        return this.profileImg;
    }
    
    public void setProfileImg(String profileImg) {
        this.profileImg = profileImg;
    }
} 