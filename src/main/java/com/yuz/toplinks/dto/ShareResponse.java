package com.yuz.toplinks.dto;

import java.util.Date;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShareResponse {
    
    private String id;
    private String shareUrl;
    private String shareToken;
    private Boolean requirePassword;
    private Integer maxDownloads;
    private Integer downloadCount;
    private Date expireTime;
    private String description;
    private Boolean isValid;
    private Date createTime;
}
