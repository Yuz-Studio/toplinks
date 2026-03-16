package com.yuz.toplinks.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuz.toplinks.entity.TlkShare;

@Mapper
public interface TlkShareMapper extends BaseMapper<TlkShare> {
    
    /**
     * 根据 token 查找分享
     */
    @Select("SELECT * FROM TLK_SHARE WHERE share_token = #{token} LIMIT 1")
    TlkShare findByToken(@Param("token") String token);
    
    /**
     * 根据文件 ID 查找所有分享
     */
    @Select("SELECT * FROM TLK_SHARE WHERE file_id = #{fileId} ORDER BY create_time DESC")
    List<TlkShare> listByFileId(@Param("fileId") String fileId);
    
    /**
     * 根据创建者查找分享
     */
    @Select("SELECT * FROM TLK_SHARE WHERE created_by = #{userId} ORDER BY create_time DESC")
    List<TlkShare> listByUserId(@Param("userId") String userId);
    
    /**
     * 查找过期的分享
     */
    @Select("SELECT * FROM TLK_SHARE WHERE status = 'active' AND expire_time IS NOT NULL AND expire_time < NOW()")
    List<TlkShare> findExpired();
    
    /**
     * 查找已达到下载次数上限的分享
     */
    @Select("SELECT * FROM TLK_SHARE WHERE status = 'active' AND max_downloads IS NOT NULL AND download_count >= max_downloads")
    List<TlkShare> findMaxDownloadsReached();
    
    /**
     * 批量清理过期分享（原子操作）
     */
    @Update("UPDATE TLK_SHARE SET status = 'inactive', update_time = NOW() WHERE status = 'active' AND expire_time IS NOT NULL AND expire_time < NOW()")
    int cleanupExpiredBatch();
    
    /**
     * 批量清理已达下载次数上限的分享（原子操作）
     */
    @Update("UPDATE TLK_SHARE SET status = 'inactive', update_time = NOW() WHERE status = 'active' AND max_downloads IS NOT NULL AND download_count >= max_downloads")
    int cleanupMaxDownloadsBatch();
    
    /**
     * 原子增加下载次数（避免并发问题）
     */
    @Update("UPDATE TLK_SHARE SET download_count = download_count + 1, update_time = NOW() WHERE share_token = #{token} AND status = 'active'")
    int incrementDownloadCountAtomic(@Param("token") String token);
    
    /**
     * 检查分享是否存在且有效
     */
    @Select("SELECT COUNT(*) FROM TLK_SHARE WHERE share_token = #{token} AND status = 'active'")
    int countByToken(@Param("token") String token);
}
