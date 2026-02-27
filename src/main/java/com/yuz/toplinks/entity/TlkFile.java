package com.yuz.toplinks.entity;

import java.util.Set;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 
 * @author yuanzhi
 * 文件存储对象
 */
@TableName("TLK_FILE")
@Data
public class TlkFile extends BaseEntity {

	//文件名称
	private String name;
	
	//文件在存储中的对象键（R2 key 或本地路径）
	private String path;
	
	//6位数字或字母组成，唯一不能重复
	private String uid;
	
	//扩展名
	private String ext;
	
	//文件大小
	private Long size;
	
	//文件hash值
	private String hash;
	
	//创建IP地址
	private String createIp;
	
	//创建用户的ID
	private String userId;

	//所属分类ID
	private String categoryId;

	//Cloudflare R2 公开访问地址
	private String cloudUrl;

	public static final Set<String> IMAGE_EXTS  = Set.of("jpg","jpeg","png","gif","webp","svg","bmp","ico");
	public static final Set<String> VIDEO_EXTS  = Set.of("mp4","mkv","avi","mov","webm","flv");
	public static final Set<String> AUDIO_EXTS  = Set.of("mp3","wav","ogg","flac","aac","m4a");
	public static final Set<String> TEXT_EXTS   = Set.of("txt","md","log","csv","json","xml");
	public static final Set<String> DOC_EXTS    = Set.of("doc","docx","xls","xlsx","ppt","pptx");
	public static final Set<String> MOBI_EXTS   = Set.of("mobi","azw","azw3");

	/**
	 * 返回文件类型标识：image / video / audio / pdf / text / document / mobi / other
	 */
	public String getFileType() {
		if (ext == null) return "other";
		String lower = ext.toLowerCase();
		if (IMAGE_EXTS.contains(lower))  return "image";
		if (VIDEO_EXTS.contains(lower))  return "video";
		if (AUDIO_EXTS.contains(lower))  return "audio";
		if ("pdf".equals(lower))         return "pdf";
		if (TEXT_EXTS.contains(lower))   return "text";
		if (DOC_EXTS.contains(lower))    return "document";
		if (MOBI_EXTS.contains(lower))   return "mobi";
		return "other";
	}

	/**
	 * 返回文件大小的可读字符串
	 */
	public String getSizeText() {
		if (size == null || size < 0) return "";
		if (size < 1024) return size + " B";
		if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
		if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
		return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
	}
}
