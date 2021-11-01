package com.sjhy.plugin.entity;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * @author lihu@eventslack.com
 * @since 2021/10/29
 */
@Getter
@Builder
public class AnnotationInfo {
    /**
     * 注解的名字
     */
    private String name;

    /**
     * 注解对应的参数列表
     */
    private Map<String, Object> annotationValues;
}
