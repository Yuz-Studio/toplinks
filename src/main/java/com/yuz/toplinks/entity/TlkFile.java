package com.yuz.toplinks.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@TableName("TLK_FILE")
@Data
public class TlkFile extends BaseEntity {

	private String name;
	
	private String path;
	
	private String uid;
	
	private String ext;
	
	private Long size;
	
	private String hash;
	
	private String createIp;
	
	private String userId;
}
