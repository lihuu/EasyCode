package com.sjhy.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiMethodImpl;
import com.sjhy.plugin.comm.TargetTestFileNotFoundException;
import com.sjhy.plugin.config.Settings;
import com.sjhy.plugin.entity.*;
import com.sjhy.plugin.service.CodeGenerateService;
import com.sjhy.plugin.tool.PsiUtils;
import org.jetbrains.annotations.NotNull;

import static com.sjhy.plugin.tool.TemplateUtils.getTemplate;

/**
 * 生成测试文件
 */
public class GenerateFenixXml extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        PsiElement psiElement = e.getData(LangDataKeys.PSI_ELEMENT);
        if (psiElement instanceof PsiMethodImpl) {
            generateByMethod(project, (PsiMethodImpl)psiElement);
        } else if (psiElement instanceof PsiClass) {
            generateByClass(project, (PsiClass)psiElement);
        } else {
            PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
            if (psiFile instanceof PsiJavaFile) {
                generateFenixFile(project, (PsiJavaFile)psiFile, "fenix.file.xml");
            }
        }
    }

    private void generateFenixFile(Project project, PsiJavaFile psiJavaFile, String templateName) {
        String classFileName = psiJavaFile.getName();
        ClassInfo classInfo = new ClassInfo(classFileName.substring(0, classFileName.indexOf(".")), psiJavaFile.getPackageName());
        TemplateGroup templateGroup = Settings.getInstance().getTemplateGroupMap().get("Fenix");
        Template template = getTemplate(templateGroup, templateName);
        CodeGenerateService.getInstance(project).generateFenixXml(template, classInfo);
    }

    private void generateByMethod(Project project, PsiMethodImpl psiMethod) {
        MethodInfo methodInfo = PsiUtils.toMethodInfo(psiMethod);
        TemplateGroup templateGroup = Settings.getInstance().getTemplateGroupMap().get("Fenix");
        Template template = getTemplate(templateGroup, "fenix.method.xml");
        CodeGenerateService instance = CodeGenerateService.getInstance(project);
        try {
            instance.generateFenixXml(template, methodInfo);
        } catch (TargetTestFileNotFoundException targetTestFileNotFoundException) {
            //可能对应的文件不存在，如果不存在就先创建
            Template testClassTemplate = getTemplate(templateGroup, "fenix.file.xml");
            instance.generateFenixXml(testClassTemplate, methodInfo.getClassInfo());
            instance.generateFenixXml(template, methodInfo);
        }
    }

    private void generateByClass(Project project, PsiClass psiClass) {
        String name = psiClass.getName();
        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName == null) {
            return;
        }
        //文件创建所有的
        ClassInfo classInfo = new ClassInfo(name, qualifiedName.substring(0, qualifiedName.lastIndexOf(".")));
        TemplateGroup templateGroup = Settings.getInstance().getTemplateGroupMap().get("Fenix");
        Template template = getTemplate(templateGroup, "fenix.file.xml");
        CodeGenerateService.getInstance(project).generateFenixXml(template, classInfo);
    }

}
