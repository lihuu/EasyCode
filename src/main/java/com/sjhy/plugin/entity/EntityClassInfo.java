package com.sjhy.plugin.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 需要生成代码的
 * 根据entity自动生成代码，这个就是那个entity类的基本信息
 *
 * @author lihu <1449488533qq@gmail.com>
 * @date 2021/4/18 20:20
 */
@Getter
@Setter
public abstract class EntityClassInfo {

    public abstract String getPackageName();


    /**
     * 表名（首字母大写）
     */
    private String name;

    public abstract List<PropertyInfo> getAllProperties();

    public abstract List<PropertyInfo> getPrimaryKeyProperties();


    /**
     * 模板组名称
     */
    private String templateGroupName;

    /**
     * 保存的包名称
     */
    private String savePackageName;
    /**
     * 保存路径
     */
    private String savePath;
    /**
     * 保存的model名称
     */
    private String saveModelName;
}
