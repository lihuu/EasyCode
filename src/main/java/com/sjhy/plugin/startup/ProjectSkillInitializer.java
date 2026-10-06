package com.sjhy.plugin.startup;

import com.intellij.ide.fileTemplates.impl.UrlUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.sjhy.plugin.config.Settings;
import com.sjhy.plugin.tool.ModuleUtils;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * 项目打开时自动创建 AI 代码生成模板 Skill 脚手架
 * <p>
 * 在项目的 .agents/skills/easy-code-templates/ 下写入 SKILL.md，任何 AI Agent 在该项目内工作
 * 时都能据此为项目生成专属的 EasyCode-lite 模板组。仅对 Java 项目生效；文件已存在时不覆盖
 * 用户修改；可通过设置页的开关关闭
 *
 * @author lihu <1449488533qq@gmail.com>
 * @since 2026/10/6
 */
public class ProjectSkillInitializer implements ProjectActivity {

    /**
     * 内置的 Skill 脚手架资源
     */
    private static final String SKILL_RESOURCE = "/skill/project-skill.md";

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        try {
            if (project.isDefault() || project.getBasePath() == null) {
                return Unit.INSTANCE;
            }
            Settings settings = Settings.getInstance();
            if (!settings.isAutoCreateProjectSkill()) {
                return Unit.INSTANCE;
            }
            Path basePath = Path.of(project.getBasePath());
            // 仅针对 Java 项目：构建文件存在或存在源码根
            boolean javaProject = Files.exists(basePath.resolve("pom.xml"))
                || Files.exists(basePath.resolve("build.gradle"))
                || Files.exists(basePath.resolve("build.gradle.kts"))
                || Arrays.stream(ModuleManager.getInstance(project).getModules()).anyMatch(ModuleUtils::existsSourcePath);
            if (!javaProject) {
                return Unit.INSTANCE;
            }
            Path skillFile = basePath.resolve(Path.of(".agents", "skills", "easy-code-templates", "SKILL.md"));
            if (Files.exists(skillFile)) {
                // 已存在则从不覆盖用户修改
                return Unit.INSTANCE;
            }
            String content = loadSkillTemplate();
            if (content == null || content.isEmpty()) {
                Logger.getInstance(ProjectSkillInitializer.class).warn("Skill 脚手架资源缺失: " + SKILL_RESOURCE);
                return Unit.INSTANCE;
            }
            Files.createDirectories(skillFile.getParent());
            Files.writeString(skillFile, content);
            Logger.getInstance(ProjectSkillInitializer.class).info("已创建 AI 模板 Skill 脚手架: " + skillFile);
        } catch (IOException e) {
            Logger.getInstance(ProjectSkillInitializer.class).warn("创建 AI 模板 Skill 脚手架失败", e);
        }
        return Unit.INSTANCE;
    }

    /**
     * 加载内置的 Skill 脚手架内容
     */
    @Nullable
    private static String loadSkillTemplate() throws IOException {
        URL url = ProjectSkillInitializer.class.getResource(SKILL_RESOURCE);
        if (url == null) {
            return null;
        }
        return UrlUtil.loadText(url);
    }
}
