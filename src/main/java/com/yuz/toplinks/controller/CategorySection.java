package com.yuz.toplinks.controller;

import java.util.List;

import com.yuz.toplinks.entity.TlkCategory;
import com.yuz.toplinks.entity.TlkFile;

/**
 * View model for a homepage category section containing the category and its preview files.
 */
public record CategorySection(TlkCategory category, List<TlkFile> files) {
}
