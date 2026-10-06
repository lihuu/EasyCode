package com.sjhy.plugin.tool;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.sjhy.plugin.config.Settings;
import com.sjhy.plugin.constants.MsgValue;
import com.sjhy.plugin.entity.TemplateGroup;
import com.sjhy.plugin.model.ProjectSettingModel;
import com.sjhy.plugin.service.ProjectLevelSettingsService;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 按项目解析模板组，模板组是应用级配置，但不同项目可能需要绑定不同的组（如 JUnit4 与 JUnit5 项目），
 * 该工具负责：缺省组可用时直接使用；不可用时自动寻找候选组并按项目记住用户的选择
 *
 * @author lihu <1449488533qq@gmail.com>
 * @since 2026/10/5
 */
public final class TemplateGroupResolver {
    /**
     * 禁止创建实例对象
     */
    private TemplateGroupResolver() {
        throw new UnsupportedOperationException();
    }

    /**
     * 解析包含所有所需模板的模板组
     * <p>
     * 解析顺序：项目记住的组（缺省组）可用时直接返回；否则扫描所有包含所需模板的组，
     * 唯一候选直接采用，多个候选弹窗让用户选择，选择结果持久化到项目设置
     *
     * @param project               项目
     * @param rememberedGroupName   项目记住的组名（含缺省值）
     * @param rememberAction        用户选定组名后的持久化动作，如 state::setTestTemplateGroupName
     * @param requiredTemplateNames 组内必须包含的模板名
     * @return 解析到的模板组，用户取消或无候选组时返回 null
     */
    @Nullable
    public static TemplateGroup resolveGroup(Project project, String rememberedGroupName, Consumer<String> rememberAction, String... requiredTemplateNames) {
        Map<String, TemplateGroup> groupMap = Settings.getInstance().getTemplateGroupMap();
        TemplateGroup rememberedGroup = groupMap.get(rememberedGroupName);
        if (containsAll(rememberedGroup, requiredTemplateNames)) {
            return rememberedGroup;
        }
        List<String> candidateGroupNames = groupMap.entrySet().stream()
            .filter(entry -> containsAll(entry.getValue(), requiredTemplateNames))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        if (candidateGroupNames.isEmpty()) {
            Messages.showErrorDialog(project, "没有找到同时包含模板 " + String.join("、", requiredTemplateNames) + " 的模板组，请先在设置中创建对应模板组", MsgValue.TITLE_INFO);
            return null;
        }
        String selectedGroupName;
        if (candidateGroupNames.size() == 1) {
            selectedGroupName = candidateGroupNames.get(0);
        } else {
            int selectedIndex = Messages.showChooseDialog(project,
                "存在多个包含模板 " + String.join("、", requiredTemplateNames) + " 的模板组，请选择本项目使用的模板组（选择后将记住）",
                MsgValue.TITLE_INFO, null, candidateGroupNames.toArray(new String[0]), candidateGroupNames.get(0));
            if (selectedIndex < 0) {
                return null;
            }
            selectedGroupName = candidateGroupNames.get(selectedIndex);
        }
        rememberGroupName(project, selectedGroupName, rememberAction);
        return groupMap.get(selectedGroupName);
    }

    /**
     * 判断组内是否包含所有所需模板
     */
    private static boolean containsAll(@Nullable TemplateGroup templateGroup, String... requiredTemplateNames) {
        if (templateGroup == null || templateGroup.getElementList() == null) {
            return false;
        }
        return Arrays.stream(requiredTemplateNames).allMatch(name -> templateGroup.getTemplate(name) != null);
    }

    /**
     * 将选定的组名持久化到项目设置
     */
    private static void rememberGroupName(Project project, String groupName, Consumer<String> rememberAction) {
        ProjectLevelSettingsService settingsService = ProjectLevelSettingsService.getInstance(project);
        ProjectSettingModel state = settingsService.getState();
        if (state == null) {
            state = new ProjectSettingModel();
        }
        rememberAction.accept(groupName);
        settingsService.loadState(state);
    }
}
