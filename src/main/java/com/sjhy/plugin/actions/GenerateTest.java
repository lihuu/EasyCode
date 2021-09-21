package com.sjhy.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiMethodImpl;
import com.sjhy.plugin.config.Settings;
import com.sjhy.plugin.entity.*;
import com.sjhy.plugin.service.CodeGenerateService;
import org.apache.commons.codec.language.bm.Lang;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        if (psiElement instanceof PsiMethodImpl) {
            //方法
            PsiMethodImpl targetMethod = (PsiMethodImpl)psiElement;
            String methodName = targetMethod.getName();
            PsiParameterList parameterList = targetMethod.getParameterList();
            PsiClass containingClass = targetMethod.getContainingClass();
            String containingClassName;
            String qualifiedName;
            if (containingClass != null) {
                containingClassName = containingClass.getName();
                qualifiedName = containingClass.getQualifiedName();
            } else {
                containingClassName = "";
                qualifiedName = "";
            }
            ClassInfo classInfo = new ClassInfo(containingClassName, qualifiedName.substring(0, qualifiedName.lastIndexOf(".")));

            MethodInfo methodInfo = MethodInfo.builder()
                .methodName(methodName)
                .containingClassName(containingClassName)
                .classInfo(classInfo)
                .methodParameters(Stream.of(parameterList.getParameters()).map(
                    psiParameter -> PropertyInfo.builder().name(psiParameter.getName()).type(psiParameter.getType().getCanonicalText())
                        .shortType(psiParameter.getType().getPresentableText()).build()).collect(Collectors.toList())).build();
            Template template =
                Settings.getInstance().getTemplateGroupMap().get("Test").getElementList().stream().filter(t -> "test.method".equals(t.getName())).findFirst()
                    .orElseThrow(() -> new RuntimeException("模块不存在"));
            CodeGenerateService.getInstance(project).generateTestCode(template, methodInfo);
        } else if (psiElement instanceof PsiClass) {

            PsiClass psiClass = (PsiClass)psiElement;
            String name = psiClass.getName();
            String qualifiedName = psiClass.getQualifiedName();
            //文件创建所有的
            ClassInfo classInfo = new ClassInfo(name, qualifiedName.substring(0, qualifiedName.lastIndexOf(".")));
            Template template = Settings.getInstance().getTemplateGroupMap().get("Test").getElementList().stream().filter(t -> "test.common".equals(t.getName())).findFirst()
                .orElseThrow(() -> new RuntimeException("模板不存在"));
            CodeGenerateService.getInstance(project).generateTestCode(template, classInfo);
        }

    }
}
