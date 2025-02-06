package com.zhang.scanner.enums;

import com.intellij.openapi.util.text.StringUtil;

public enum SqlTypeEnum {
    /**
     * oracle
     */
    ORACLE("oracle", "Oracle"),

    MYSQL("mysql", "MySQL"),

    GAUSS("gauss", "Gauss"),

    POSTGRESQL("postgresql", "PostgreSQL"),

    DB2("db2", "");


    private String code;
    private String desc;

    SqlTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static SqlTypeEnum parse(String code) {
        for (SqlTypeEnum item : values()) {
            if (StringUtil.equals(code, item.code)) {
                return item;
            }
        }
        throw new RuntimeException("Enum code not exist.");
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
