package com.yuz.toplinks.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@TableName("TLK_CATEGORY")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TlkCategory extends BaseEntity {

	private String name;

	private String description;

	// Bootstrap icon class name, e.g. "bi-image"
	private String icon;

	private Integer sortOrder;
}
