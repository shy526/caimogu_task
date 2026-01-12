package com.github.shy526.vo;

import lombok.Data;

@Data
public class UserInfo {
    private String Uid;
    private String Nickname;
    private String ASuthorization;
    private Integer Point = -1;

    private Integer MaxGame = 3;
    private Integer MaxGameComment = 1;
    private Integer MaxComment = 3;
}
