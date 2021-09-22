package com.sjhy.plugin.ui;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ExceptionUtil;
import com.sjhy.plugin.config.Settings;
import com.sjhy.plugin.constants.MsgValue;
import com.sjhy.plugin.constants.StrState;
import com.sjhy.plugin.entity.EntityClassInfo;
import com.sjhy.plugin.entity.Template;
import com.sjhy.plugin.entity.TemplateGroup;
import com.sjhy.plugin.model.ProjectSettingModel;
import com.sjhy.plugin.service.CodeGenerateService;
import com.sjhy.plugin.service.ProjectLevelSettingsService;
import com.sjhy.plugin.tool.CurrGroupUtils;
import com.sjhy.plugin.tool.ModuleUtils;
import com.sjhy.plugin.tool.ProjectUtils;
import com.sjhy.plugin.tool.StringUtils;
import com.sjhy.plugin.ui.base.ChoosePathListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 根据实体类生成对应的文件，和根据数据库的表生成文件的一样
 *
 * @author lihu <1449488533qq@gmail.com>
 * @date 2021/4/18 18:01
 */
public class CodeGenerateForm extends JDialog {
    private final Project project;
    private final CodeGenerateService codeGenerateService;
    private TemplateGroup templateGroup;
    private final LinkedList<Module> moduleList;
    private JPanel contentPannel;
    private JButton ok;
    private JButton cancel;
    private JComboBox<String> groupBox;
    private JComboBox<String> moduleBox;
    private JTextField packageField;
    private JButton choosePackageButton;
    private JTextField pathField;
    private JButton choosePathButton;
    private JCheckBox allSelect;
    private JCheckBox generateTests;
    private JPanel templatesPannel;
    private final EntityClassInfo entityClassInfo;

    /**
     * 所有模板复选框
     */
    private final List<JCheckBox> checkBoxList = new ArrayList<>();

    /**
     * 获取选中的Module
     *
     * @return 选中的Module
     */
    private Module getSelectModule() {
        String name = (String)moduleBox.getSelectedItem();
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        return ModuleManager.getInstance(project).findModuleByName(name);
    }

    public CodeGenerateForm(Project project, EntityClassInfo entityClassInfo) {
        this.project = project;
        this.entityClassInfo = entityClassInfo;
        this.codeGenerateService = CodeGenerateService.getInstance(project);
        this.templateGroup = CurrGroupUtils.getCurrTemplateGroup();
        // 初始化module，存在资源路径的排前面
        this.moduleList = new LinkedList<>();
        String lastedSelectedModuleName =
            Optional.ofNullable(ProjectLevelSettingsService.getInstance(project).getState()).orElse(new ProjectSettingModel()).getLastedSelectedModuleName();
        if (StringUtils.isEmpty(lastedSelectedModuleName)) {
            for (Module module : ModuleManager.getInstance(project).getModules()) {
                // 存在源代码文件夹放前面，否则放后面
                if (ModuleUtils.existsSourcePath(module)) {
                    this.moduleList.add(0, module);
                } else {
                    this.moduleList.add(module);
                }
            }
        } else {
            //上次选择的模块放在第一个
            for (Module module : ModuleManager.getInstance(project).getModules()) {
                // 存在源代码文件夹放前面，否则放后面
                if (Objects.equals(lastedSelectedModuleName, module.getName())) {
                    this.moduleList.add(0, module);
                } else {
                    this.moduleList.add(module);
                }
            }
        }
        init();
        setTitle("代码生成");
        setContentPane(contentPannel);
        setModal(true);
        getRootPane().setDefaultButton(ok);
        cancel.addActionListener((event) -> onCancel());
        ok.addActionListener((event) -> onOk());
    }

    /**
     * 获取基本路径
     *
     * @return 基本路径
     */
    private String getBasePath() {
        Module module = getSelectModule();
        VirtualFile baseVirtualFile = ProjectUtils.getBaseDir(project);
        if (baseVirtualFile == null) {
            Messages.showWarningDialog("无法获取到项目基本路径！", MsgValue.TITLE_INFO);
            return "";
        }
        String baseDir = baseVirtualFile.getPath();
        if (module != null) {
            VirtualFile virtualFile = ModuleUtils.getSourcePath(module);
            if (virtualFile != null) {
                baseDir = virtualFile.getPath();
            }
        }
        return baseDir;
    }

    /**
     * 刷新目录
     */
    private void refreshPath() {
        String packageName = packageField.getText();
        // 获取基本路径
        String path = getBasePath();
        // 兼容Linux路径
        path = path.replace("\\", "/");
        // 如果存在包路径，添加包路径
        if (!StringUtils.isEmpty(packageName)) {
            path += "/" + packageName.replace(".", "/");
        }
        pathField.setText(path);
    }

    private void init() {
        initTemplates();
        //初始化项目模块的，例如 maven 中的模块
        for (Module module : this.moduleList) {
            moduleBox.addItem(module.getName());
        }
        //监听module选择事件
        moduleBox.addActionListener(e -> {
            // 刷新路径
            Module selectModule = getSelectModule();
            if (selectModule != null) {
                ProjectLevelSettingsService projectLevelSettingsService = ProjectLevelSettingsService.getInstance(project);
                ProjectSettingModel state = projectLevelSettingsService.getState();
                if (state == null) {
                    state = new ProjectSettingModel();
                }
                state.setLastedSelectedModuleName(selectModule.getName());
                projectLevelSettingsService.loadState(state);
            }

            refreshPath();
        });

        if (!StringUtils.isEmpty(entityClassInfo.getPackageName())) {
            //设置默认的package name
            packageField.setText(entityClassInfo.getPackageName());
        }

        try {
            Class<?> cls = Class.forName("com.intellij.ide.util.PackageChooserDialog");
            //添加包选择事件
            choosePackageButton.addActionListener(e -> {
                try {
                    Constructor<?> constructor = cls.getConstructor(String.class, Project.class);
                    Object dialog = constructor.newInstance("Package Chooser", project);
                    // 显示窗口
                    Method showMethod = cls.getMethod("show");
                    showMethod.invoke(dialog);
                    // 获取选中的包名
                    Method getSelectedPackageMethod = cls.getMethod("getSelectedPackage");
                    Object psiPackage = getSelectedPackageMethod.invoke(dialog);
                    if (psiPackage != null) {
                        Method getQualifiedNameMethod = psiPackage.getClass().getMethod("getQualifiedName");
                        String packageName = (String)getQualifiedNameMethod.invoke(psiPackage);
                        packageField.setText(packageName);
                        // 刷新路径
                        refreshPath();
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e1) {
                    ExceptionUtil.rethrow(e1);
                }
            });

            // 添加包编辑框失去焦点事件
            packageField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    // 刷新路径
                    refreshPath();
                }
            });
        } catch (ClassNotFoundException e) {
            // 没有PackageChooserDialog，并非支持Java的IDE，禁用相关UI组件
            packageField.setEnabled(false);
            choosePackageButton.setEnabled(false);
        }

        //初始化路径
        refreshPath();

        //选择保存路径的事件处理
        choosePathButton.addActionListener((event) -> {
            new ChoosePathListener(project, getSelectModule(), pathField).onChoose(event);
        });

        Settings settings = Settings.getInstance();
        String groupName = settings.getCurrTemplateGroupName();
        if (!StringUtils.isEmpty(entityClassInfo.getTemplateGroupName())) {
            if (settings.getTemplateGroupMap().containsKey(entityClassInfo.getTemplateGroupName())) {
                groupName = entityClassInfo.getTemplateGroupName();
                this.templateGroup = settings.getTemplateGroupMap().get(groupName);
                // 选中的模板组发生变化，尝试重新初始化
                initTemplates();
            }
        }
        for (String key : settings.getTemplateGroupMap().keySet()) {
            groupBox.addItem(key);
        }
        groupBox.setSelectedItem(groupName);
        groupBox.addActionListener(e -> {
            String selectedItem = (String)groupBox.getSelectedItem();
            if (this.templateGroup.getName().equals(selectedItem)) {
                return;
            }
            this.templateGroup = settings.getTemplateGroupMap().get(selectedItem);
            // 选中的模板组发生变化，尝试重新初始化
            initTemplates();
            this.open();
        });
        String savePath = entityClassInfo.getSavePath();
        if (!StringUtils.isEmpty(savePath)) {
            // 判断是否需要拼接项目路径
            if (savePath.startsWith(StrState.RELATIVE_PATH)) {
                String projectPath = project.getBasePath();
                savePath = projectPath + savePath.substring(1);
            }
            pathField.setText(savePath);
        }

    }

    private void initTemplates() {
        checkBoxList.clear();
        templatesPannel.removeAll();
        templatesPannel.setLayout(new GridLayout(6, 2));
        templateGroup.getElementList().forEach(template -> {
            if (template.isShow()) {
                JCheckBox checkBox = new JCheckBox(template.getName());
                checkBoxList.add(checkBox);
                templatesPannel.add(checkBox);
            }
        });
        // 移除所有旧事件
        ActionListener[] actionListeners = allSelect.getActionListeners();
        if (actionListeners != null && actionListeners.length > 0) {
            for (ActionListener actionListener : actionListeners) {
                allSelect.removeActionListener(actionListener);
            }
        }
        //添加全选事件
        allSelect.addActionListener(e -> checkBoxList.forEach(jCheckBox -> jCheckBox.setSelected(allSelect.isSelected())));
        allSelect.setSelected(false);
    }

    /**
     * 获取已经选中的模板
     *
     * @return 模板对象集合
     */
    private List<Template> getSelectTemplate() {
        boolean generateTests = this.generateTests.isSelected();
        // 获取到已选择的复选框
        List<String> selectTemplateNameList = checkBoxList.stream().filter(JCheckBox::isSelected).map(JCheckBox::getText).collect(Collectors.toList());
        // 将复选框转换成对应的模板对象，如果勾选了生成测试用例，也加入
        return selectTemplateNameList.stream().flatMap(name -> {
            if (generateTests) {
                return Stream.of(templateGroup.getTemplate(name), templateGroup.getTemplate("test." + name));
            } else {
                return Stream.of(templateGroup.getTemplate(name));
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private void onOk() {
        List<Template> selectTemplateList = getSelectTemplate();
        // 如果选择的模板是空的
        if (selectTemplateList.isEmpty()) {
            Messages.showWarningDialog("Can't Select Template!", MsgValue.TITLE_INFO);
            return;
        }
        String savePath = pathField.getText();
        if (StringUtils.isEmpty(savePath)) {
            Messages.showWarningDialog("Can't Select Save Path!", MsgValue.TITLE_INFO);
            return;
        }
        // 针对Linux系统路径做处理
        savePath = savePath.replace("\\", "/");
        // 保存路径使用相对路径
        String basePath = project.getBasePath();
        if (!StringUtils.isEmpty(basePath) && savePath.startsWith(basePath)) {
            if (savePath.length() > basePath.length()) {
                if ("/".equals(savePath.substring(basePath.length(), basePath.length() + 1))) {
                    savePath = savePath.replace(basePath, ".");
                }
            } else {
                savePath = savePath.replace(basePath, ".");
            }
        }
        // 保存配置
        entityClassInfo.setSavePath(savePath);
        entityClassInfo.setSavePackageName(packageField.getText());
        entityClassInfo.setTemplateGroupName((String)groupBox.getSelectedItem());
        // 生成代码
        codeGenerateService.generate(getSelectTemplate(), entityClassInfo);
        // 关闭窗口
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public void open() {
        this.pack();
        setLocationRelativeTo(null);
        this.setVisible(true);
    }
}
