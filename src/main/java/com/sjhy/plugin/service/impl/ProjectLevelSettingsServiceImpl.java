package com.sjhy.plugin.service.impl;

import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.sjhy.plugin.model.ProjectSettingModel;
import com.sjhy.plugin.service.ProjectLevelSettingsService;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author lihu <1449488533qq@gmail.com>
 * @since 2021/9/21
 */
@Data
@State(name = "EasyCodeProjectSetting", storages = @Storage("easy-code-project-setting.xml"))
public class ProjectLevelSettingsServiceImpl implements ProjectLevelSettingsService {
    private ProjectSettingModel projectSettingModel = new ProjectSettingModel();

    public ProjectLevelSettingsServiceImpl() {
        projectSettingModel.setBaseSrcPath("");
        projectSettingModel.setBaseTestSrcPath("");
    }


    @Override
    public @Nullable ProjectSettingModel getState() {
        return this.projectSettingModel;
    }

    @Override
    public void loadState(@NotNull ProjectSettingModel state) {
        XmlSerializerUtil.copyBean(state, this.projectSettingModel);
    }
}
