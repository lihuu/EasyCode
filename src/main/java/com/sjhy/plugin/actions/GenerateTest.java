package com.sjhy.plugin.actions;

import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue;
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
import com.sjhy.plugin.tool.ModuleUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
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
            generateTestMethod(project, (PsiMethodImpl)psiElement);
        } else if (psiElement instanceof PsiClass psiClass) {
            generateTestClass(project, psiClass);
        } else {
            PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
            if (psiFile instanceof PsiJavaFile) {
                generateTestFile(project, (PsiJavaFile)psiFile);
            }
        }
    }

    private void generateTestFile(Project project, PsiJavaFile psiJavaFile) {
        String classFileName = psiJavaFile.getName();
        String modulePath = ModuleUtils.getModulePath(psiJavaFile);
        String moduleName = ModuleUtils.getModuleName(psiJavaFile);
        String name = classFileName.substring(0, classFileName.indexOf("."));
        String packageName = psiJavaFile.getPackageName();
        ClassInfo classInfo = new ClassInfo(name, modulePath, packageName, moduleName);
        Template template = getTemplate("test.common");
        CodeGenerateService.getInstance(project).generateTestCode(template, classInfo);
    }

    private void generateTestMethod(Project project, PsiMethodImpl psiMethod) {
        String methodName = psiMethod.getName();
        PsiAnnotation[] annotations = psiMethod.getAnnotations();
        //解析方法上面的注解
        List<AnnotationInfo> methodAnnotationInfoList = buildAnnotationInfoList(annotations);
        PsiParameterList parameterList = psiMethod.getParameterList();
        PsiClass containingClass = psiMethod.getContainingClass();
        String containingClassName;
        String qualifiedName;
        if (containingClass != null) {
            containingClassName = containingClass.getName();
            qualifiedName = Optional.ofNullable(containingClass.getQualifiedName()).orElse("");
        } else {
            containingClassName = "";
            qualifiedName = "";
        }
        String modulePath = ModuleUtils.getModulePath(psiMethod);
        String packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
        String moduleName = ModuleUtils.getModuleName(psiMethod);
        ClassInfo classInfo = new ClassInfo(containingClassName, modulePath, packageName, moduleName);
        classInfo.setOpenFile(false);
        classInfo.setAnnotationInfoList(buildAnnotationInfoList(containingClass.getAnnotations()));
        MethodInfo methodInfo = MethodInfo.builder()
            .methodName(methodName)
            .containingClassName(containingClassName)
            .classInfo(classInfo)
            .annotationInfos(methodAnnotationInfoList)
            .methodParameters(toMethodParameters(parameterList)
            ).build();
        Template template = getTemplate("test.method");
        try {
            CodeGenerateService.getInstance(project).generateTestCode(template, methodInfo);
        } catch (TargetTestFileNotFoundException targetTestFileNotFoundException) {
            //可能对应的文件不存在，如果不存在就先创建
            Template testClassTemplate = getTemplate("test.common");
            CodeGenerateService.getInstance(project).generateTestCode(testClassTemplate, methodInfo.getClassInfo());
            CodeGenerateService.getInstance(project).generateTestCode(template, methodInfo);
        }
    }

    private Template getTemplate(String templateName) {
        return Settings.getInstance().getTemplateGroupMap().get("Test")
            .getElementList().stream()
            .filter(t -> templateName.equals(t.getName()))
            .findFirst().orElseThrow(() -> new RuntimeException("模板内容不存在"));
    }

    @NotNull
    private List<PropertyInfo> toMethodParameters(PsiParameterList parameterList) {
        return Stream.of(parameterList.getParameters()).map(toProperInfo()).collect(Collectors.toList());
    }

    @NotNull
    private Function<PsiParameter, PropertyInfo> toProperInfo() {
        return psiParameter -> PropertyInfo.builder()
            .name(psiParameter.getName())
            .type(psiParameter.getType().getCanonicalText())
            .shortType(psiParameter.getType().getPresentableText())
            .build();
    }

    private void generateTestClass(Project project, PsiClass psiClass) {
        String name = psiClass.getName();
        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName == null) {
            return;
        }
        //文件创建所有的
        String modulePath = ModuleUtils.getModulePath(psiClass);
        String packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
        String moduleName = ModuleUtils.getModuleName(psiClass);
        ClassInfo classInfo = new ClassInfo(name, modulePath, packageName, moduleName);
        Template template = getTemplate("test.common");
        CodeGenerateService.getInstance(project).generateTestCode(template, classInfo);
    }

    private List<AnnotationInfo> buildAnnotationInfoList(PsiAnnotation[] psiAnnotations) {
        try {
            return Arrays.stream(psiAnnotations).map(psiAnnotation -> {
                String annotationQualifiedName = psiAnnotation.getQualifiedName();
                Map<String, Object> annotationValues = Arrays.stream(psiAnnotation.getParameterList().getAttributes())
                    .filter(psiNameValuePair -> psiNameValuePair.getAttributeValue() != null && psiNameValuePair.getAttributeValue() instanceof JvmAnnotationConstantValue)
                    .collect(Collectors.toMap(PsiNameValuePair::getAttributeName,
                        psiNameValuePair -> Optional.ofNullable(((JvmAnnotationConstantValue)psiNameValuePair.getAttributeValue()).getConstantValue()).orElse(new Object()),
                        (v1, v2) -> v2));
                return AnnotationInfo.builder().name(annotationQualifiedName).annotationValues(annotationValues).build();
            }).collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
