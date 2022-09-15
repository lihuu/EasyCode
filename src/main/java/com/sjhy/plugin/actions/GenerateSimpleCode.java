package com.sjhy.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.sjhy.plugin.entity.ClassInfo;
import com.sjhy.plugin.entity.PropertyInfo;
import com.sjhy.plugin.ui.CodeGenerateForm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 可以根据entity自动生成对应的代码，不用从数据库中获取
 *
 * @author lihu <1449488533qq@gmail.com>
 * @since  2021/4/18 17:52
 */
public class GenerateSimpleCode extends AnAction {
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
        String classFileName = psiJavaFile.getName();
        ClassInfo classInfo = new ClassInfo(classFileName.substring(0, classFileName.indexOf(".")), psiJavaFile.getPackageName());
        Stream<PsiField> psiFieldStream = Arrays.stream(psiJavaFile.getClasses()[0].getAllFields());
        List<PsiField> allFields = psiFieldStream.collect(Collectors.toList());
        List<PsiField> psiFieldList = allFields.stream().filter(f -> f.hasAnnotation("javax.persistence.Id")).collect(Collectors.toList());
        if (!psiFieldList.isEmpty()) {
            classInfo.setPrimaryKeyProperties(psiFieldList.stream().map(GenerateSimpleCode::toPropertyInfo).collect(Collectors.toList()));
        }
        classInfo.setAllProperties(allFields.stream().map(GenerateSimpleCode::toPropertyInfo).collect(Collectors.toList()));
        new CodeGenerateForm(project, classInfo).open();
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
