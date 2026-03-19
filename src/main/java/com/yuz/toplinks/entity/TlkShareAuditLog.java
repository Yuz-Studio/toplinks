package com.yuz.toplinks.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Builder;
import lombok.Data;

/**
 * 分享访问审计日志
 * @author yuanzhi
 */
@TableName("TLK_SHARE_AUDIT_LOG")
@Data
@Builder
public class TlkShareAuditLog {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    /** 分享 ID */
    private String shareId;
    
    /** 分享 Token */
    private String shareToken;
    
    /** 访问者 IP */
    private String visitorIp;
    
    /** 访问类型：view/download/password_attempt */
    private String actionType;
    
    /** 是否成功 */
    private Boolean success;
    
    /** 失败原因 */
    private String failureReason;
    
    /** User-Agent */
    private String userAgent;
    
    /** 创建时间 */
    private Date createTime;
    
    public static final String ACTION_VIEW = "view";
    public static final String ACTION_DOWNLOAD = "download";
    public static final String ACTION_PASSWORD_ATTEMPT = "password_attempt";
}
