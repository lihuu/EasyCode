package com.sjhy.plugin.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import com.intellij.database.psi.DbTable;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 表信息
 *
 * @author makejava
 * @version 1.0.0
 * @since 2018/07/17 13:10
 */
@Getter
@Setter
public class TableInfo extends EntityClassInfo {
    /**
     * 原始对象
     */
    @JsonIgnore
    private DbTable obj;

    /**
     * 表名前缀
     */
    private String preName;
    /**
     * 注释
     */
    private String comment;

    /**
     * 所有列
     */
    private List<ColumnInfo> fullColumn;
    /**
     * 主键列
     */
    private List<ColumnInfo> pkColumn;
    /**
     * 其他列
     */
    private List<ColumnInfo> otherColumn;

    @Override
    public String getPackageName() {
        return "";
    }

    @Override
    public List<PropertyInfo> getAllProperties() {
        return fullColumn.stream().map(columnInfo -> PropertyInfo.builder()
            .name(columnInfo.getName())
            .type(columnInfo.getType())
            .shortType(columnInfo.getShortType())
            .build()).collect(Collectors.toList());
    }

    @Override
    public List<PropertyInfo> getPrimaryKeyProperties() {
        return pkColumn.stream().map(columnInfo -> PropertyInfo.builder()
            .name(columnInfo.getName())
            .type(columnInfo.getType())
            .shortType(columnInfo.getShortType())
            .build()).collect(Collectors.toList());
    }
}
