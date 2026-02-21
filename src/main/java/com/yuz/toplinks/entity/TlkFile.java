package com.yuz.toplinks.entity;

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
	
	//文件的真实路径
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
}
