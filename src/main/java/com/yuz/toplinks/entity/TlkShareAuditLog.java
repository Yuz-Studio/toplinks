package com.yuz.toplinks.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分享访问审计日志
 * @author yuanzhi
 */
@TableName("TLK_SHARE_AUDIT_LOG")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private Boolean isSuccess;
    
    /** 失败原因 */
    private String failureReason;
    
    /** User-Agent */
    private String userAgent;
    
    /** 创建时间 */
    private Date createTime;
    
    // Explicit getters/setters for Lombok compatibility
    public Boolean getIsSuccess() { return isSuccess; }
    public void setIsSuccess(Boolean isSuccess) { this.isSuccess = isSuccess; }
    
    public static final String ACTION_VIEW = "view";
    public static final String ACTION_DOWNLOAD = "download";
    public static final String ACTION_PASSWORD_ATTEMPT = "password_attempt";
}
