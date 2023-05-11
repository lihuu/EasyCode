package com.sjhy.plugin.tool;

import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue;
import com.intellij.psi.*;
import com.sjhy.plugin.entity.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Pis对象和项目中对象的转换工具类
 *
 * @author lihu
 * @since 2023/5/11 09:59
 */
public class PsiUtils {

    public static PropertyInfo toPropertyInfo(PsiParameter psiParameter) {
        return PropertyInfo.builder()
            .name(psiParameter.getName())
            .type(psiParameter.getType().getCanonicalText())
            .shortType(psiParameter.getType().getPresentableText())
            .build();
    }

    @NotNull
    public static List<PropertyInfo> toMethodParameters(PsiParameterList parameterList) {
        return Stream.of(parameterList.getParameters()).map(PsiUtils::toPropertyInfo).collect(Collectors.toList());
    }

    public static List<AnnotationInfo> toAnnotationInfoList(PsiAnnotation[] psiAnnotations) {
        try {
            return Arrays.stream(psiAnnotations).map(PsiUtils::toAnnotationInfo).collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static AnnotationInfo toAnnotationInfo(PsiAnnotation psiAnnotation) {
        String annotationQualifiedName = psiAnnotation.getQualifiedName();
        Map<String, Object> annotationValues = Arrays.stream(psiAnnotation.getParameterList().getAttributes())
            .filter(psiNameValuePair -> psiNameValuePair.getAttributeValue() != null && psiNameValuePair.getAttributeValue() instanceof JvmAnnotationConstantValue)
            .collect(Collectors.toMap(PsiNameValuePair::getAttributeName,
                psiNameValuePair -> Optional.ofNullable(((JvmAnnotationConstantValue)psiNameValuePair.getAttributeValue()).getConstantValue()).orElse(new Object()),
                (v1, v2) -> v2));
        return AnnotationInfo.builder().name(annotationQualifiedName).annotationValues(annotationValues).build();
    }

    public static MethodInfo toMethodInfo(PsiMethod psiMethod) {
        PsiAnnotation[] annotations = psiMethod.getAnnotations();
        //解析方法上面的注解
        List<AnnotationInfo> methodAnnotationInfoList = toAnnotationInfoList(annotations);
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
        String methodName = psiMethod.getName();
        ClassInfo classInfo = new ClassInfo(containingClassName, qualifiedName.substring(0, qualifiedName.lastIndexOf(".")));
        classInfo.setOpenFile(false);
        classInfo.setAnnotationInfoList(methodAnnotationInfoList);
        return MethodInfo.builder()
            .methodName(methodName)
            .containingClassName(containingClassName)
            .classInfo(classInfo)
            .annotationInfos(methodAnnotationInfoList)
            .methodParameters(toMethodParameters(parameterList)
            ).build();
    }

}
