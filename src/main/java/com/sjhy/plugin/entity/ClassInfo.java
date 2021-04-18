package com.sjhy.plugin.entity;

/**
 * 需要生成代码的
 * 根据entity自动生成代码，这个就是那个entity类的基本信息
 *
 * @author lihu <1449488533qq@gmail.com>
 * @date 2021/4/18 20:20
 */
public class ClassInfo extends TableInfo {
    private final String name;
    private final String packageName;


    public ClassInfo(String className, String packageName) {
        this.name = className;
        super.setName(name);
        this.packageName = packageName;
    }

}
