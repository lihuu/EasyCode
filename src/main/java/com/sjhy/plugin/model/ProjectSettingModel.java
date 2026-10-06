package com.sjhy.plugin.model;

import com.sjhy.plugin.tool.StringUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * @author lihu@eventslack.com
 * @since 2021/9/22
 */
@Getter
@Setter
public class ProjectSettingModel {

    /**
     * 生成测试文件的时候，默认的保存目录
     */
    private String baseTestSrcPath;

    /**
     * 生成文件的默认保存目录，不用再选择了
     */
    private String baseSrcPath;

    /**
     * fenix 文件的保存目录
     */
    private String baseFenixPath;

    /**
     * 记住上次选择的模块（如果有多个模块的情况下）
     */
    private String lastedSelectedModuleName;

    /**
     * 上次选择的模板组
     */
    private String lastSelectedTemplateGroup;

    /**
     * 生成测试文件使用的模板组名
     */
    private String testTemplateGroupName;

    /**
     * 生成 fenix xml 使用的模板组名
     */
    private String fenixTemplateGroupName;

    private Map<String, String> moduleTestSrcMap;

    public String getLastSelectedTemplateGroup() {
        if (StringUtils.isEmpty(lastSelectedTemplateGroup)) {
            return "Default";
        } else {
            return lastSelectedTemplateGroup;
        }
    }

    public String getTestTemplateGroupName() {
        if (StringUtils.isEmpty(testTemplateGroupName)) {
            return "Test";
        } else {
            return testTemplateGroupName;
        }
    }

    public String getFenixTemplateGroupName() {
        if (StringUtils.isEmpty(fenixTemplateGroupName)) {
            return "Fenix";
        } else {
            return fenixTemplateGroupName;
        }
    }

}
