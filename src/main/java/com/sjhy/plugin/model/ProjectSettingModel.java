package com.sjhy.plugin.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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
     * 记住上次选择的模块（如果有多个模块的情况下）
     */
    private String lastedSelectedModuleName;
}
