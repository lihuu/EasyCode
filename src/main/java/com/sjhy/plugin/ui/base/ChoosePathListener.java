package com.sjhy.plugin.ui.base;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.sjhy.plugin.tool.ModuleUtils;
import com.sjhy.plugin.tool.ProjectUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author lihu <1449488533qq@gmail.com>
 * @date 2021/4/18 21:54
 */
public class ChoosePathListener {
    private final Project project;
    private final Module module;
    private final JTextField pathField;


    public ChoosePathListener(Project project, Module module, JTextField pathField) {
        this.project = project;
        this.module = module;
        this.pathField = pathField;
    }

   public void onChoose(ActionEvent event){
        VirtualFile path = ProjectUtils.getBaseDir(project);
        if (module != null) {
            path = ModuleUtils.getSourcePath(module);
        }
        VirtualFile virtualFile = FileChooser.chooseFile(FileChooserDescriptorFactory.createSingleFolderDescriptor(), project, path);
        if (virtualFile != null) {
            pathField.setText(virtualFile.getPath());
        }
    }

}
