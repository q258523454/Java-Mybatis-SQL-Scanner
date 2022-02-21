package com.zhang.scanner.enums;

import org.apache.commons.lang3.StringUtils;

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
            if (StringUtils.equals(code, item.code)) {
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
