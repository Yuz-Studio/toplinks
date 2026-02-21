package com.yuz.toplinks.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@TableName("TLK_CATEGORY")
@Data
public class TlkCategory extends BaseEntity {

	private String name;

	private String description;

	// Bootstrap icon class name, e.g. "bi-image"
	private String icon;

	private Integer sortOrder;
}
