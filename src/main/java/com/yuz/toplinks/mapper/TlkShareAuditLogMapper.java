package com.yuz.toplinks.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuz.toplinks.entity.TlkShareAuditLog;

@Mapper
public interface TlkShareAuditLogMapper extends BaseMapper<TlkShareAuditLog> {
    
    /**
     * 根据分享 ID 查找审计日志
     */
    @Select("SELECT * FROM TLK_SHARE_AUDIT_LOG WHERE share_id = #{shareId} ORDER BY create_time DESC LIMIT 100")
    List<TlkShareAuditLog> listByShareId(@Param("shareId") String shareId);
    
    /**
     * 根据 Token 查找最近的审计日志
     */
    @Select("SELECT * FROM TLK_SHARE_AUDIT_LOG WHERE share_token = #{token} ORDER BY create_time DESC LIMIT 50")
    List<TlkShareAuditLog> listByToken(@Param("token") String token);
    
    /**
     * 统计指定 IP 最近的失败尝试次数
     */
    @Select("SELECT COUNT(*) FROM TLK_SHARE_AUDIT_LOG WHERE visitor_ip = #{ip} AND success = false AND action_type = 'password_attempt' AND create_time > DATE_SUB(NOW(), INTERVAL 1 HOUR)")
    int countFailedAttemptsByIp(@Param("ip") String ip);
}
