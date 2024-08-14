package com.sjhy.plugin.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.sjhy.plugin.config.Settings;
import com.sjhy.plugin.entity.*;
import com.sjhy.plugin.model.ProjectSettingModel;
import com.sjhy.plugin.service.CodeGenerateService;
import com.sjhy.plugin.service.ProjectLevelSettingsService;
import com.sjhy.plugin.tool.*;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.SystemIndependent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author makejava
 * @version 1.0.0
 * @since 2018/09/02 12:50
 */
public class CodeGenerateServiceImpl implements CodeGenerateService {
    /**
     * 项目对象
     */
    private final Project project;
    /**
     * 模型管理
     */
    private final ModuleManager moduleManager;
    /**
     * 表信息服务
     */
    /**
     * 导入包时过滤的包前缀
     */
    private static final String FILTER_PACKAGE_NAME = "java.lang";

    public CodeGenerateServiceImpl(Project project) {
        this.project = project;
        this.moduleManager = ModuleManager.getInstance(project);
    }

    /**
     * 生成代码，并自动保存到对应位置
     *
     * @param templates     模板
     * @param tableInfoList 表信息对象
     * @param title         是否显示提示
     * @param otherParam    其他参数
     */
    public void generate(Collection<Template> templates, Collection<TableInfo> tableInfoList, boolean title, Map<String, Object> otherParam) {
        generate(templates, tableInfoList, title, otherParam, false);
    }

    /**
     * 生成代码，并自动保存到对应位置
     *
     * @param templates     模板
     * @param tableInfoList 表信息对象
     * @param title         是否显示提示
     * @param otherParam    其他参数
     */
    public void generate(Collection<Template> templates, Collection<TableInfo> tableInfoList, boolean title, Map<String, Object> otherParam, boolean generateTests) {
        if (CollectionUtil.isEmpty(templates) || CollectionUtil.isEmpty(tableInfoList)) {
            return;
        }
        // 处理模板，注入全局变量（克隆一份，防止篡改）
        templates = CloneUtils.cloneByJson(templates, new TypeReference<ArrayList<Template>>() {
        });
        TemplateUtils.addGlobalConfig(templates);
        // 生成代码
        for (TableInfo tableInfo : tableInfoList) {
            // 表名去除前缀
            // 构建参数
            Map<String, Object> param = getDefaultParam();
            // 其他参数
            if (otherParam != null) {
                param.putAll(otherParam);
            }
            // 所有表信息对象
            param.put("tableInfoList", tableInfoList);
            // 表信息对象
            param.put("tableInfo", tableInfo);
            // 设置模型路径与导包列表
            setModulePathAndImportList(param, tableInfo);
            // 设置额外代码生成服务
            param.put("generateService", new ExtraCodeGenerateUtils(this, tableInfo, title));
            for (Template template : templates) {
                saveFile(param, template, tableInfo, title);
            }
        }
    }

    private void saveFile(Map<String, Object> param, Template template, EntityClassInfo entityClassInfo, boolean title) {
        Callback callback = new Callback();
        // 设置回调对象
        param.put("callback", callback);
        // 开始生成
        String code = VelocityUtils.generate(template.getCode(), param);
        // 清除前面空格
        StringBuilder sb = new StringBuilder(code);
        while (sb.length() > 0 && Character.isWhitespace(sb.charAt(0))) {
            sb.deleteCharAt(0);
        }
        code = sb.toString();
        // 设置一个默认保存路径与默认文件名
        if (StringUtils.isEmpty(callback.getFileName())) {
            callback.setFileName(entityClassInfo.getName() + "Default.java");
        }
        if (StringUtils.isEmpty(callback.getSavePath())) {
            callback.setSavePath(entityClassInfo.getSavePath());
        }
        String path = callback.getSavePath();
        path = path.replace("\\", "/");
        // 针对相对路径进行处理
        if (path.startsWith(".")) {
            path = project.getBasePath() + path.substring(1);
        }
        new SaveFile(project, path, callback.getFileName(), code, callback.isReformat(), title, false, false).write();
    }

    private void saveFile(Map<String, Object> param, Template template, MethodInfo methodInfo, String defaultSavePath) {
        Callback callback = new Callback();
        // 设置回调对象
        param.put("callback", callback);
        // 开始生成
        String code = VelocityUtils.generate(template.getCode(), param);
        // 清除前面空格
        StringBuilder sb = new StringBuilder(code);
        while (sb.length() > 0 && Character.isWhitespace(sb.charAt(0))) {
            sb.deleteCharAt(0);
        }
        code = sb.toString();
        // 设置一个默认保存路径与默认文件名
        if (StringUtils.isEmpty(callback.getFileName())) {
            callback.setFileName(methodInfo.getContainingClassName() + "Test.java");
        }

        if (StringUtils.isEmpty(callback.getSavePath())) {
            callback.setSavePath(defaultSavePath);
        }
        String path = callback.getSavePath();
        path = path.replace("\\", "/");
        // 针对相对路径进行处理
        if (path.startsWith(".")) {
            path = project.getBasePath() + path.substring(1);
        }
        new SaveFile(project, path, callback.getFileName(), code, callback.isReformat(), false, true, true).write();
    }

    private void saveFile(Map<String, Object> param, Template template, ClassInfo classInfo, String defaultSavePath) {
        Callback callback = new Callback();
        // 设置回调对象
        param.put("callback", callback);
        // 开始生成
        String code = VelocityUtils.generate(template.getCode(), param);
        // 清除前面空格
        StringBuilder sb = new StringBuilder(code);
        while (sb.length() > 0 && Character.isWhitespace(sb.charAt(0))) {
            sb.deleteCharAt(0);
        }
        code = sb.toString();
        // 设置一个默认保存路径与默认文件名
        if (StringUtils.isEmpty(callback.getFileName())) {
            callback.setFileName(classInfo.getName() + "Test.java");
        }
        if (StringUtils.isEmpty(callback.getSavePath())) {
            callback.setSavePath(defaultSavePath);
        }
        String path = callback.getSavePath();
        path = path.replace("\\", "/");
        // 针对相对路径进行处理
        if (path.startsWith(".")) {
            path = project.getBasePath() + path.substring(1);
        }
        new SaveFile(project, path, callback.getFileName(), code, callback.isReformat(), false, false, classInfo.isOpenFile()).write();
    }

    private String getDefaultTestSrcSavePath(ProjectLevelSettingsService projectLevelSettingsService, @Nullable @SystemIndependent @NonNls String basePath) {
        //设置默认的保存的目录
        //TODO 如果是多个模块的，默认取第一个
        ProjectSettingModel state = projectLevelSettingsService.getState();
        String baseSavePath;
        if (state != null && !StringUtils.isEmpty(state.getBaseTestSrcPath())) {
            baseSavePath = state.getBaseTestSrcPath();
        } else {
            baseSavePath = basePath + "/src/test/java/";
        }
        return baseSavePath;
    }

    private String getDefaultFenixXmlSavePath(Project project) {
        //设置默认的保存的目录
        ProjectLevelSettingsService projectLevelSettingsService = ProjectLevelSettingsService.getInstance(project);
        ProjectSettingModel state = projectLevelSettingsService.getState();
        String baseSavePath;
        if (state != null && !StringUtils.isEmpty(state.getBaseFenixPath())) {
            baseSavePath = state.getBaseFenixPath();
        } else {
            baseSavePath = project.getBasePath() + "/src/main/resources/";
            state = new ProjectSettingModel();
            state.setBaseFenixPath(baseSavePath);
            projectLevelSettingsService.loadState(state);
        }
        return baseSavePath;
    }


    /**
     * 生成代码
     *
     * @param template  模板
     * @param tableInfo 表信息对象
     * @return 生成好的代码
     */
    @Override
    public String generate(Template template, TableInfo tableInfo) {
        // 获取默认参数
        Map<String, Object> param = getDefaultParam();
        // 表信息对象，进行克隆，防止篡改
        param.put("tableInfo", tableInfo);
        // 设置模型路径与导包列表
        setModulePathAndImportList(param, tableInfo);
        // 处理模板，注入全局变量
        TemplateUtils.addGlobalConfig(template);
        return VelocityUtils.generate(template.getCode(), param).trim();
    }

    @Override
    public void generate(List<Template> templates, EntityClassInfo entityClassInfo) {
        for (Template template : templates) {
            Map<String, Object> param = getDefaultParam();
            param.put("classInfo", entityClassInfo);
            param.put("tableInfo", entityClassInfo);
            param.put("entityClassInfo", entityClassInfo);
            saveFile(param, template, entityClassInfo, false);
        }
    }

    @Override
    public void generateTestCode(Template template, MethodInfo methodInfo) {
        String modulePath = methodInfo.getClassInfo().getModulePath();
        String defaultSavePath =
            getDefaultTestSrcSavePath(ProjectLevelSettingsService.getInstance(project), modulePath) + methodInfo.getClassInfo().getPackageName().replace(".", "/");
        generateCodeByMethodInfo(template, methodInfo, defaultSavePath);
    }


    private Map<String, Map<String, Object>> toAnnotationMap(List<AnnotationInfo> annotationInfos) {
        if (CollectionUtils.isEmpty(annotationInfos)) {
            return Collections.emptyMap();
        }
        return annotationInfos.stream().collect(Collectors.toMap(annotationInfo -> this.annotationSimpleName(annotationInfo.getName()), AnnotationInfo::getAnnotationValues, (v1, v2) -> {
            v1.putAll(v2);
            return v1;
        }));
    }

    private String annotationSimpleName(String name) {
        return name.substring(name.lastIndexOf(".")+1);
    }

    @Override
    public void generateTestCode(Template template, ClassInfo classInfo) {
        String modulePath = classInfo.getModulePath();
        String defaultSavePath = getDefaultTestSrcSavePath(ProjectLevelSettingsService.getInstance(project), modulePath) + classInfo.getPackageName().replace(".", "/");
        generateCodeByClassInfo(template, classInfo, defaultSavePath);
    }

    private void generateCodeByClassInfo(Template template, ClassInfo classInfo, String defaultSavePath) {
        Map<String, Object> param = getDefaultParam();
        param.put("classInfo", classInfo);
        saveFile(param, template, classInfo, defaultSavePath);
    }

    @Override
    public void generateFenixXml(Template template, MethodInfo methodInfo) {
        generateCodeByMethodInfo(template, methodInfo, getDefaultFenixXmlSavePath(project));
    }

    @Override
    public void generateFenixXml(Template template, ClassInfo classInfo) {
        generateCodeByClassInfo(template, classInfo, getDefaultFenixXmlSavePath(project));
    }

    /**
     * 设置模型路径与导包列表
     *
     * @param param     参数
     * @param tableInfo 表信息对象
     */
    private void setModulePathAndImportList(Map<String, Object> param, TableInfo tableInfo) {
        Module module = null;
        if (!StringUtils.isEmpty(tableInfo.getSaveModelName())) {
            module = this.moduleManager.findModuleByName(tableInfo.getSaveModelName());
        }
        if (module != null) {
            // 设置modulePath
            param.put("modulePath", ModuleUtils.getModuleDir(module).getPath());
        }
        // 设置要导入的包
        param.put("importList", getImportList(tableInfo));
    }

    /**
     * 获取默认参数
     *
     * @return 参数
     */
    private Map<String, Object> getDefaultParam() {
        // 系统设置
        Settings settings = Settings.getInstance();
        Map<String, Object> param = new HashMap<>(20);
        // 作者
        param.put("author", settings.getAuthor());
        //工具类
        param.put("tool", GlobalTool.getInstance());
        param.put("time", TimeUtils.getInstance());
        // 项目路径
        param.put("projectPath", project.getBasePath());
        return param;
    }

    /**
     * 获取导入列表
     *
     * @param tableInfo 表信息对象
     * @return 导入列表
     */
    private Set<String> getImportList(TableInfo tableInfo) {
        // 创建一个自带排序的集合
        Set<String> result = new TreeSet<>();
        tableInfo.getFullColumn().forEach(columnInfo -> {
            if (!columnInfo.getType().startsWith(FILTER_PACKAGE_NAME)) {
                result.add(columnInfo.getType());
            }
        });
        return result;
    }

    private void generateCodeByMethodInfo(Template template, MethodInfo methodInfo, String defaultSavePath) {
        Map<String, Object> param = getDefaultParam();
        param.put("methodInfo", methodInfo);
        param.put("classInfo",methodInfo.getClassInfo());
        param.put("methodAnnotationMap", toAnnotationMap(methodInfo.getAnnotationInfos()));
        param.put("classAnnotationMap", toAnnotationMap(methodInfo.getClassInfo().getAnnotationInfoList()));
        String parameters = methodInfo.getMethodParameters().stream().map(PropertyInfo::getType).collect(Collectors.joining(","));
        param.put("parameters", parameters);
        saveFile(param, template, methodInfo, defaultSavePath);
    }
}
