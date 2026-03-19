package com.yuz.toplinks.service;

import java.util.Date;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yuz.toplinks.entity.TlkShareAuditLog;
import com.yuz.toplinks.mapper.TlkShareAuditLogMapper;

import lombok.RequiredArgsConstructor;

/**
 * 分享审计服务
 * @author yuanzhi
 */
@Service
@RequiredArgsConstructor
public class ShareAuditService {
    
    private final TlkShareAuditLogMapper auditLogMapper;
    
    /**
     * 记录访问日志
     */
    @Transactional
    public void logAccess(String shareId, String shareToken, String visitorIp, 
                         String actionType, boolean success, String failureReason, 
                         String userAgent) {
        TlkShareAuditLog log = TlkShareAuditLog.builder()
            .shareId(shareId)
            .shareToken(shareToken)
            .visitorIp(visitorIp)
            .actionType(actionType)
            .success(success)
            .failureReason(failureReason)
            .userAgent(userAgent)
            .createTime(new Date())
            .build();
        auditLogMapper.insert(log);
    }
    
    /**
     * 检查 IP 是否被限制（1 小时内失败超过 5 次）
     */
    public boolean isIpRateLimited(String ip) {
        int failedAttempts = auditLogMapper.countFailedAttemptsByIp(ip);
        return failedAttempts >= 5;
    }
    
    /**
     * 获取指定 IP 的失败尝试次数
     */
    public int getFailedAttemptsByIp(String ip) {
        return auditLogMapper.countFailedAttemptsByIp(ip);
    }
}
