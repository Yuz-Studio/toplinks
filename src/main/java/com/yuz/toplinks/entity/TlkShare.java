package com.yuz.toplinks.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * 文件分享实体
 * @author yuanzhi
 */
@TableName("TLK_SHARE")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TlkShare {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    /** 关联文件 ID */
    private String fileId;
    
    /** 分享 token（32 位随机） */
    private String shareToken;
    
    /** 分享密码（加密存储） */
    private String sharePassword;
    
    /** 是否需要密码 */
    private Boolean requirePassword;
    
    /** 最大下载次数（null=不限） */
    private Integer maxDownloads;
    
    /** 已下载次数 */
    private Integer downloadCount;
    
    /** 过期时间 */
    private Date expireTime;
    
    /** 创建者 ID */
    private String createdBy;
    
    /** 分享描述 */
    private String description;
    
    /** 状态：active/inactive */
    private String status;
    
    /** 创建时间 */
    private Date createTime;
    
    /** 更新时间 */
    private Date updateTime;
    
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_INACTIVE = "inactive";
    
    /**
     * 获取分享链接
     */
    @JsonIgnore
    public String getShareUrl() {
        return "/share/" + this.shareToken;
    }
    
    /**
     * 检查分享是否有效
     */
    @JsonIgnore
    public boolean isValid() {
        if (!STATUS_ACTIVE.equals(this.status)) {
            return false;
        }
        if (this.expireTime != null && new Date().after(this.expireTime)) {
            return false;
        }
        if (this.maxDownloads != null && this.downloadCount >= this.maxDownloads) {
            return false;
        }
        return true;
    }
}
