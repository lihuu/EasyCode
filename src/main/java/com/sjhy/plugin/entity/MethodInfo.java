package com.sjhy.plugin.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 方法信息
 *
 * @author lihu <1449488533qq@gmail.com>
 * @date 2021/4/20 22:18
 */
@Getter
@Builder
public class MethodInfo {
    /**
     * 方法的名字
     */
    private final String methodName;
    /**
     * 方法的参数
     */
    private final List<PropertyInfo> methodParameters;

    private final List<AnnotationInfo> annotationInfos;

    /**
     * 方法对应的类的名字
     */
    private final String containingClassName;

    private final ClassInfo classInfo;

}
