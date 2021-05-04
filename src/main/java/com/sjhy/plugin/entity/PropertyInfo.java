package com.sjhy.plugin.entity;

import lombok.Builder;
import lombok.Getter;

/**
 * 字段信息
 *
 * @author lihu <1449488533qq@gmail.com>
 * @date 2021/4/20 22:18
 */
@Getter
@Builder
public class PropertyInfo {
    /**
     * 字段的名字
     */
    private final String name;
    /**
     * 字段的类型，例如 java.lang.Long
     */
    private final String type;
    /**
     * 字段的短类型，例如 Long
     */
    private final String shortType;
}
