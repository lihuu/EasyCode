package com.sjhy.plugin.service;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.sjhy.plugin.entity.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * 代码生成服务，Project级别Service
 *
 * @author makejava
 * @version 1.0.0
 * @since 2018/09/02 12:48
 */
public interface CodeGenerateService {
    /**
     * 获取实例对象
     *
     * @param project 项目对象
     * @return 实例对象
     */
    static CodeGenerateService getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, CodeGenerateService.class);
    }

    /**
     * 生成代码
     *
     * @param template  模板
     * @param tableInfo 表信息对象
     * @return 生成好的代码
     */
    String generate(Template template, TableInfo tableInfo);

    /**
     * 生成代码
     *
     * @param template
     * @param entityClassInfo Entity Class
     * @return
     */
    void generate(List<Template> template, EntityClassInfo entityClassInfo);

    /**
     * 生成一个方法的测试测试代码
     *
     * @param template
     * @param methodInfo
     */
    void generateTestCode(Template template, MethodInfo methodInfo);

    /**
     * 生成Java类的测试代码
     *
     * @param template
     * @param classInfo
     */
    void generateTestCode(Template template, ClassInfo classInfo);

    /**
     * 生成一个方法的测试测试代码
     *
     * @param template
     * @param methodInfo
     */
    void generateFenixXml(Template template, MethodInfo methodInfo);

    /**
     * 生成Java类的测试代码
     *
     * @param template
     * @param classInfo
     */
    void generateFenixXml(Template template, ClassInfo classInfo);
}
