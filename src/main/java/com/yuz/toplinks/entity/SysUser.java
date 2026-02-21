package com.yuz.toplinks.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@TableName("SYS_USER")
@Data
public class SysUser extends BaseEntity {

	private String username;

	private String email;

	// BCrypt hashed; nullable for OAuth-only users
	private String password;

	private String googleId;

	private String avatar;

	private String nickname;
}
