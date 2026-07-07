package com.sirinsirin.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenUserInfoDto implements Serializable {

    private static final long serialVersionUID = 9170480547933408839L;
    private String userId;
    private String nickName;
    private String avatar;
    private Long expireAt;
    private String token;

    private Integer fansCount;
    private Integer currentCoinCount;
    private Integer focusCount;
}
