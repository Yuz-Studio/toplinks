package com.yuz.toplinks.dto;

import java.util.Date;

import lombok.Data;

@Data
public class ShareCreateRequest {
    
    /** 文件 ID */
    private String fileId;
    
    /** 是否需要密码 */
    private boolean requirePassword;
    
    /** 分享密码 */
    private String password;
    
    /** 最大下载次数（null=不限） */
    private Integer maxDownloads;
    
    /** 过期时间 */
    private Date expireTime;
    
    /** 分享描述 */
    private String description;
}
