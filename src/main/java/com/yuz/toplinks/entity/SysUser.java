package com.yuz.toplinks.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@TableName("SYS_USER")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SysUser extends BaseEntity {

	private String username;

	private String email;

	// BCrypt hashed; nullable for OAuth-only users
	private String password;

	private String googleId;

	private String avatar;

	private String nickname;
}
