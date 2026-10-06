package com.sjhy.plugin.actions;

import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiMethodImpl;
import com.sjhy.plugin.comm.TargetTestFileNotFoundException;
import com.sjhy.plugin.entity.*;
import com.sjhy.plugin.model.ProjectSettingModel;
import com.sjhy.plugin.service.CodeGenerateService;
import com.sjhy.plugin.service.ProjectLevelSettingsService;
import com.sjhy.plugin.tool.TemplateGroupResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            generateByMethod(project, (PsiMethodImpl)psiElement, "fenix.method.xml", "fenix.file.xml");
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
        ClassInfo classInfo = new ClassInfo(simpleNameOf(classFileName), psiJavaFile.getPackageName());
        TemplateGroup templateGroup = resolveTemplateGroup(project, templateName);
        if (templateGroup == null) {
            return;
        }
        Template template = getRequiredTemplate(templateGroup, templateName);
        CodeGenerateService.getInstance(project).generateFenixXml(template, classInfo);
    }

    private void generateByMethod(Project project, PsiMethodImpl psiMethod, String methodTemplateName, String fileTemplateName) {
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
        ClassInfo classInfo = new ClassInfo(containingClassName, packageNameOf(qualifiedName));
        classInfo.setOpenFile(false);
        classInfo.setAnnotationInfoList(buildAnnotationInfoList(containingClass.getAnnotations()));
        MethodInfo methodInfo = MethodInfo.builder()
            .methodName(methodName)
            .containingClassName(containingClassName)
            .classInfo(classInfo)
            .annotationInfos(methodAnnotationInfoList)
            .methodParameters(toMethodParameters(parameterList)
            ).build();
        TemplateGroup templateGroup = resolveTemplateGroup(project, methodTemplateName, fileTemplateName);
        if (templateGroup == null) {
            return;
        }
        Template template = getRequiredTemplate(templateGroup, methodTemplateName);
        try {
            CodeGenerateService.getInstance(project).generateFenixXml(template, methodInfo);
        } catch (TargetTestFileNotFoundException targetTestFileNotFoundException) {
            //可能对应的文件不存在，如果不存在就先创建
            Template testClassTemplate = getRequiredTemplate(templateGroup, fileTemplateName);
            CodeGenerateService.getInstance(project).generateFenixXml(testClassTemplate, methodInfo.getClassInfo());
            CodeGenerateService.getInstance(project).generateFenixXml(template, methodInfo);
        }
    }

    /**
     * 按项目解析包含所需模板的模板组，缺省使用 Fenix 组
     */
    @Nullable
    private static TemplateGroup resolveTemplateGroup(Project project, String... requiredTemplateNames) {
        ProjectSettingModel state = ProjectLevelSettingsService.getInstance(project).getState();
        if (state == null) {
            state = new ProjectSettingModel();
        }
        return TemplateGroupResolver.resolveGroup(project, state.getFenixTemplateGroupName(), state::setFenixTemplateGroupName, requiredTemplateNames);
    }

    private static Template getRequiredTemplate(TemplateGroup templateGroup, String templateName) {
        Template template = templateGroup.getTemplate(templateName);
        if (template == null) {
            throw new RuntimeException("模板内容不存在：" + templateName);
        }
        return template;
    }

    /**
     * 去掉文件扩展名
     */
    private static String simpleNameOf(String classFileName) {
        int dotIndex = classFileName.indexOf(".");
        return dotIndex > 0 ? classFileName.substring(0, dotIndex) : classFileName;
    }

    /**
     * 从全限定名中解析包名，默认包（无包名）返回空串
     */
    private static String packageNameOf(String qualifiedName) {
        int lastDotIndex = qualifiedName.lastIndexOf(".");
        return lastDotIndex < 0 ? "" : qualifiedName.substring(0, lastDotIndex);
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

    private void generateByClass(Project project, PsiClass psiClass) {
        String name = psiClass.getName();
        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName == null) {
            return;
        }
        //文件创建所有的
        ClassInfo classInfo = new ClassInfo(name, packageNameOf(qualifiedName));
        TemplateGroup templateGroup = resolveTemplateGroup(project, "fenix.file.xml");
        if (templateGroup == null) {
            return;
        }
        Template template = getRequiredTemplate(templateGroup, "fenix.file.xml");
        CodeGenerateService.getInstance(project).generateFenixXml(template, classInfo);
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
