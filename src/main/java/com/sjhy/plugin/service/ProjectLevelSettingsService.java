package com.sjhy.plugin.service;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.sjhy.plugin.model.ProjectSettingModel;

/**
 * @author lihu@eventslack.com
 * @since 2021/9/22
 */
public interface ProjectLevelSettingsService extends PersistentStateComponent<ProjectSettingModel> {
    /**
     * @param project
     * @return
     */
    static ProjectLevelSettingsService getInstance(Project project) {
        return ServiceManager.getService(project, ProjectLevelSettingsService.class);
    }
}
