package com.sjhy.plugin.entity;

import lombok.Builder;
import lombok.Getter;

/**
 * @author lihu <1449488533qq@gmail.com>
 * @date 2021/4/20 22:18
 */
@Getter
@Builder
public class PropertyInfo {
    private final String name;
    private final String type;
    private final String shortType;
}
