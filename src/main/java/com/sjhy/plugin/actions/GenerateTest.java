package com.sjhy.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.sjhy.plugin.comm.TargetTestFileNotFoundException;
import com.sjhy.plugin.entity.ClassInfo;
import com.sjhy.plugin.entity.MethodInfo;
import com.sjhy.plugin.entity.Template;
import com.sjhy.plugin.service.CodeGenerateService;
import com.sjhy.plugin.tool.PsiUtils;
import com.sjhy.plugin.tool.TemplateUtils;
import org.jetbrains.annotations.NotNull;

/**
 * 生成测试文件
 */
public class GenerateTest extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        PsiElement psiElement = e.getData(LangDataKeys.PSI_ELEMENT);
        if (psiElement instanceof PsiMethod) {
            generateTestMethod(project, (PsiMethod)psiElement);
        } else if (psiElement instanceof PsiClass) {
            generateTestClass(project, (PsiClass)psiElement);
        } else {
            PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
            if (psiFile instanceof PsiJavaFile) {
                generateTestFile(project, (PsiJavaFile)psiFile);
            }
        }
    }

    private static void generateTestFile(Project project, PsiJavaFile psiJavaFile) {
        String classFileName = psiJavaFile.getName();
        ClassInfo classInfo = new ClassInfo(classFileName.substring(0, classFileName.indexOf(".")), psiJavaFile.getPackageName());
        Template template = TemplateUtils.getTemplate(project, "test.common.java");
        CodeGenerateService.getInstance(project).generateTestCode(template, classInfo);
    }

    private static void generateTestMethod(Project project, PsiMethod psiMethod) {
        MethodInfo methodInfo = PsiUtils.toMethodInfo(psiMethod);
        CodeGenerateService instance = CodeGenerateService.getInstance(project);
        try {
            Template template = TemplateUtils.getTemplate(project, "test.method.java");
            instance.generateTestCode(template, methodInfo);
        } catch (TargetTestFileNotFoundException targetTestFileNotFoundException) {
            //可能对应的文件不存在，如果不存在就先创建
            Template testClassTemplate = TemplateUtils.getTemplate(project, "test.common.java");
            instance.generateTestCode(testClassTemplate, methodInfo.getClassInfo());
            Template template = TemplateUtils.getTemplate(project, "test.method.java");
            instance.generateTestCode(template, methodInfo);
        }
    }

    private static void generateTestClass(Project project, PsiClass psiClass) {
        String name = psiClass.getName();
        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName == null) {
            return;
        }
        //文件创建所有的
        ClassInfo classInfo = new ClassInfo(name, qualifiedName.substring(0, qualifiedName.lastIndexOf(".")));
        Template template = TemplateUtils.getTemplate(project, "test.common.java");
        CodeGenerateService.getInstance(project).generateTestCode(template, classInfo);
    }

}
