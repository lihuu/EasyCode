package com.sjhy.plugin.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author lihu <1449488533qq@gmail.com>
 * @date 2021/4/20 22:36
 */
@Getter
@Setter
public class ClassInfo extends EntityClassInfo {
    private String name;
    private String packageName;
    private List<PropertyInfo> allProperties;
    private List<PropertyInfo> primaryKeyProperties;
    private List<AnnotationInfo> annotationInfoList;
    private boolean openFile = true;
    private String modulePath;

    public ClassInfo(String name, String packageName) {
        this.name = name;
        this.packageName = packageName;
    }

    public ClassInfo(String name, String modulePath, String packageName) {
        this.name = name;
        this.modulePath = modulePath;
        this.packageName = packageName;
    }

    @Override
    public String getPackageName() {
        return this.packageName;
    }

    @Override
    public String getName() {
        return this.name;
    }

}
