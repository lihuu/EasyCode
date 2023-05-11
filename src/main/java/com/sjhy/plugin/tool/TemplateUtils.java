package com.sjhy.plugin.tool;

import com.intellij.openapi.project.Project;
import com.sjhy.plugin.config.Settings;
import com.sjhy.plugin.entity.GlobalConfig;
import com.sjhy.plugin.entity.Template;
import com.sjhy.plugin.entity.TemplateGroup;
import com.sjhy.plugin.model.ProjectSettingModel;
import com.sjhy.plugin.service.ProjectLevelSettingsService;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

/**
 * 模板工具，主要用于对模板进行预处理
 *
 * @author makejava
 * @version 1.0.0
 * @since 2018/09/01 15:07
 */
public final class TemplateUtils {
    /**
     * 不允许创建实例对象
     */
    private TemplateUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * 向模板中注入全局变量
     *
     * @param template      模板
     * @param globalConfigs 全局变量
     * @return 处理好的模板
     */
    public static String addGlobalConfig(String template, Collection<GlobalConfig> globalConfigs) {
        if (CollectionUtil.isEmpty(globalConfigs)) {
            return template;
        }
        for (GlobalConfig globalConfig : globalConfigs) {
            String name = globalConfig.getName();
            // 正则被替换字符转义处理
            String value = globalConfig.getValue().replace("$", "\\$");

            // 将不带{}的变量加上{}
            template = template.replaceAll("\\$!?" + name + "(\\W)", "\\$!{" + name + "}$1");
            // 统一替换
            template = template.replaceAll("\\$!?\\{" + name + "}", value);
        }
        return template;
    }

    /**
     * 向模板中注入全局变量
     *
     * @param template      模板对象
     * @param globalConfigs 全局变量
     */
    public static void addGlobalConfig(Template template, Collection<GlobalConfig> globalConfigs) {
        if (template == null || StringUtils.isEmpty(template.getCode())) {
            return;
        }
        // 模板后面添加换行符号，防止在模板末尾添加全局变量导致无法匹配问题
        template.setCode(addGlobalConfig(template.getCode() + "\n", globalConfigs));
    }

    /**
     * 向模板中注入全局变量
     *
     * @param templates     多个模板
     * @param globalConfigs 全局变量
     */
    public static void addGlobalConfig(Collection<Template> templates, Collection<GlobalConfig> globalConfigs) {
        if (CollectionUtil.isEmpty(templates)) {
            return;
        }
        templates.forEach(template -> addGlobalConfig(template, globalConfigs));
    }

    /**
     * 向模板中注入全局变量
     *
     * @param templates 多个模板
     */
    public static void addGlobalConfig(Collection<Template> templates) {
        addGlobalConfig(templates, CurrGroupUtils.getCurrGlobalConfigGroup().getElementList());
    }

    /**
     * 向模板中注入全局变量
     *
     * @param template 单个模板
     */
    public static void addGlobalConfig(Template template) {
        if (template != null) {
            addGlobalConfig(Collections.singleton(template));
        }
    }

    public static Template getTemplate(TemplateGroup templateGroup, String templateName) {
        return templateGroup.getElementList().stream()
            .filter(t -> templateName.equals(t.getName()))
            .findFirst().orElseThrow(() -> new RuntimeException("模板内容不存在"));
    }

    public static Template getTemplate(Project project, String templateName) {
        return getTemplate(getTemplateGroup(project), templateName);
    }

    private static TemplateGroup getTemplateGroup(Project project) {
        ProjectLevelSettingsService service = ProjectLevelSettingsService.getInstance(project);
        String groupName = Objects.requireNonNull(service.getState()).getLastSelectedTemplateGroup();
        return Settings.getInstance().getTemplateGroupMap().get(groupName);
    }
}
