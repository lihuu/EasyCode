package com.sjhy.plugin.actions;

import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiMethodImpl;
import com.sjhy.plugin.comm.TargetTestFileNotFoundException;
import com.sjhy.plugin.config.Settings;
import com.sjhy.plugin.entity.*;
import com.sjhy.plugin.service.CodeGenerateService;
import org.apache.commons.codec.language.bm.Lang;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;
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
            PsiMethodImpl targetMethod = (PsiMethodImpl) psiElement;
            String methodName = targetMethod.getName();
            PsiAnnotation[] annotations = targetMethod.getAnnotations();
            //解析方法上面的注解
            List<AnnotationInfo> methodAnnotationInfoList = buildAnnotationInfoList(annotations);
            PsiParameterList parameterList = targetMethod.getParameterList();
            PsiClass containingClass = targetMethod.getContainingClass();
            String containingClassName;
            String qualifiedName;
            if (containingClass != null) {
                containingClassName = containingClass.getName();
                qualifiedName = Optional.ofNullable(containingClass.getQualifiedName()).orElse("");
            } else {
                containingClassName = "";
                qualifiedName = "";
            }
            ClassInfo classInfo = new ClassInfo(containingClassName, qualifiedName.substring(0, qualifiedName.lastIndexOf(".")));
            classInfo.setOpenFile(false);
            classInfo.setAnnotationInfoList(buildAnnotationInfoList(containingClass.getAnnotations()));
            MethodInfo methodInfo = MethodInfo.builder()
                    .methodName(methodName)
                    .containingClassName(containingClassName)
                    .classInfo(classInfo)
                    .annotationInfos(methodAnnotationInfoList)
                    .methodParameters(Stream.of(parameterList.getParameters()).map(
                            psiParameter -> 
                                PropertyInfo.builder().name(psiParameter.getName()).type(psiParameter.getType().getCanonicalText())
                                    .shortType(psiParameter.getType().getPresentableText()).build()).collect(Collectors.toList())
                    ).build();
            Template template =
                    Settings.getInstance().getTemplateGroupMap().get("Test").getElementList().stream().filter(t -> "test.method".equals(t.getName())).findFirst()
                            .orElseThrow(() -> new RuntimeException("test.method 模板文件不存在"));
            try {
                CodeGenerateService.getInstance(project).generateTestCode(template, methodInfo);
            } catch (TargetTestFileNotFoundException targetTestFileNotFoundException) {
                //可能对应的文件不存在，如果不存在就先创建
                Template testClassTemplate =
                        Settings.getInstance().getTemplateGroupMap().get("Test").getElementList().stream().filter(t -> "test.common".equals(t.getName())).findFirst()
                                .orElseThrow(() -> new RuntimeException("test.common 模板文件不存在"));
                CodeGenerateService.getInstance(project).generateTestCode(testClassTemplate, methodInfo.getClassInfo());
                CodeGenerateService.getInstance(project).generateTestCode(template, methodInfo);
            }

        } else if (psiElement instanceof PsiClass) {
            generateTestClass(project, (PsiClass)psiElement);
        } else {
            PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
            if (psiFile instanceof PsiJavaFile) {
                PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
                String classFileName = psiJavaFile.getName();
                ClassInfo classInfo = new ClassInfo(classFileName.substring(0, classFileName.indexOf(".")), psiJavaFile.getPackageName());
                Template template = Settings.getInstance().getTemplateGroupMap().get("Test").getElementList().stream().filter(t -> "test.common".equals(t.getName())).findFirst()
                        .orElseThrow(() -> new RuntimeException("模板不存在"));
                CodeGenerateService.getInstance(project).generateTestCode(template, classInfo);
            }
        }
    }

    private void generateTestClass(Project project, PsiClass psiElement) {
        String name = psiElement.getName();
        String qualifiedName = psiElement.getQualifiedName();
        if (qualifiedName == null) {
            return;
        }
        //文件创建所有的
        ClassInfo classInfo = new ClassInfo(name, qualifiedName.substring(0, qualifiedName.lastIndexOf(".")));
        Template template = Settings.getInstance().getTemplateGroupMap().get("Test").getElementList().stream().filter(t -> "test.common".equals(t.getName())).findFirst()
                .orElseThrow(() -> new RuntimeException("模板不存在"));
        CodeGenerateService.getInstance(project).generateTestCode(template, classInfo);
    }

    private List<AnnotationInfo> buildAnnotationInfoList(PsiAnnotation[] psiAnnotations) {
        try {
            return Arrays.stream(psiAnnotations).map(psiAnnotation -> {
                String annotationQualifiedName = psiAnnotation.getQualifiedName();
                Map<String, Object> annotationValues = Arrays.stream(psiAnnotation.getParameterList().getAttributes())
                        .filter(psiNameValuePair -> psiNameValuePair.getAttributeValue() != null && psiNameValuePair.getAttributeValue() instanceof JvmAnnotationConstantValue)
                        .collect(Collectors.toMap(PsiNameValuePair::getAttributeName, psiNameValuePair -> Optional.ofNullable(((JvmAnnotationConstantValue) psiNameValuePair.getAttributeValue()).getConstantValue()).orElse(new Object()), (v1, v2) -> v2));
                return AnnotationInfo.builder().name(annotationQualifiedName).annotationValues(annotationValues).build();
            }).collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
