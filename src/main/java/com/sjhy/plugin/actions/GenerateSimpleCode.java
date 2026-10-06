package com.sjhy.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.sjhy.plugin.entity.ClassInfo;
import com.sjhy.plugin.entity.PropertyInfo;
import com.sjhy.plugin.tool.ModuleUtils;
import com.sjhy.plugin.ui.CodeGenerateForm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 可以根据entity自动生成对应的代码，不用从数据库中获取
 *
 * @author lihu <1449488533qq@gmail.com>
 * @since  2021/4/18 17:52
 */
public class GenerateSimpleCode extends AnAction {

    /**
     * 识别为主键的注解全限定名，覆盖 javax/jakarta 的 JPA 注解与 MyBatis-Plus 注解
     */
    private static final List<String> PRIMARY_KEY_ANNOTATIONS = Arrays.asList(
        "javax.persistence.Id",
        "jakarta.persistence.Id",
        "javax.persistence.EmbeddedId",
        "jakarta.persistence.EmbeddedId",
        "com.baomidou.mybatisplus.annotation.TableId"
    );

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        //获取触发事件的文件
        PsiJavaFile psiJavaFile = getPsiJavaFile(e);
        if (psiJavaFile == null) {
            return;
        }
        PsiClass[] classes = psiJavaFile.getClasses();
        if (classes.length == 0) {
            return;
        }
        String classFileName = psiJavaFile.getName();
        ClassInfo classInfo = new ClassInfo(simpleNameOf(classFileName), ModuleUtils.getModulePath(psiJavaFile), psiJavaFile.getPackageName());
        List<PsiField> allFields = Arrays.stream(classes[0].getAllFields()).toList();
        List<PsiField> psiFieldList = allFields.stream().filter(GenerateSimpleCode::hasPrimaryKeyAnnotation).toList();
        if (!psiFieldList.isEmpty()) {
            classInfo.setPrimaryKeyProperties(psiFieldList.stream().map(GenerateSimpleCode::toPropertyInfo).collect(Collectors.toList()));
        }
        classInfo.setAllProperties(allFields.stream().map(GenerateSimpleCode::toPropertyInfo).collect(Collectors.toList()));
        new CodeGenerateForm(project, classInfo).open();
    }

    private static boolean hasPrimaryKeyAnnotation(PsiField psiField) {
        return PRIMARY_KEY_ANNOTATIONS.stream().anyMatch(psiField::hasAnnotation);
    }

    /**
     * 去掉文件扩展名
     */
    private static String simpleNameOf(String classFileName) {
        int dotIndex = classFileName.indexOf(".");
        return dotIndex > 0 ? classFileName.substring(0, dotIndex) : classFileName;
    }

    private static PropertyInfo toPropertyInfo(PsiField psiField) {
        String name = psiField.getName();
        String type = psiField.getType().getCanonicalText();
        return PropertyInfo.builder()
            .type(type)
            .shortType(type.substring(type.lastIndexOf(".") + 1))
            .name(name)
            .build();
    }

    @Nullable
    private PsiJavaFile getPsiJavaFile(@NotNull AnActionEvent e) {
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        if (psiFile == null) {
            return null;
        }
        PsiJavaFile psiJavaFile = null;
        if (psiFile instanceof PsiJavaFile) {
            psiJavaFile = (PsiJavaFile)psiFile;
        }
        return psiJavaFile;
    }
}
