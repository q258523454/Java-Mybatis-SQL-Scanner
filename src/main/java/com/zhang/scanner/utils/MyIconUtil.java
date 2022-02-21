package com.zhang.scanner.utils;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public class MyIconUtil {
    public static final Icon SCANNER = IconLoader.getIcon("/icons/scanner.svg", MyIconUtil.class);

    public static final Icon LIGHTNING_DARK = IconLoader.getIcon("/icons/idea/bullet.png", MyIconUtil.class);

    // 参考: AllIcons.General.Tip
    public static final Icon TEXT = IconLoader.getIcon("/icons/idea/balloonInformation.png", MyIconUtil.class);

    public static final Icon BLOCKED = IconLoader.getIcon("/icons/degreed/blocked.svg", MyIconUtil.class);
    // 参考: AllIcons.General.Tip
    public static final Icon CRITICAL = IconLoader.getIcon("/icons/degreed/critical.svg", MyIconUtil.class);
    public static final Icon MARJOR = IconLoader.getIcon("/icons/degreed/major.svg", MyIconUtil.class);

    /**
     * 数据库相关图标
     */
    public static final class DataBase {
        // AllIcons.Providers.Mysql;
        public static final Icon MYSQL = IconLoader.getIcon("/icons/sqltype/mysql.svg", MyIconUtil.class);

        // AllIcons.Providers.Oracle;
        public static final Icon ORACLE = IconLoader.getIcon("/icons/sqltype/oracle.svg", MyIconUtil.class);

        // AllIcons.Providers.Postgresql;
        public static final Icon POSTGRESQL = IconLoader.getIcon("/icons/sqltype/postgresql.svg", MyIconUtil.class);

        // AllIcons.Providers.DB2;
        public static final Icon DB2 = IconLoader.getIcon("/icons/sqltype/DB2.svg", MyIconUtil.class);

        public static final Icon GAUSS = IconLoader.getIcon("/icons/sqltype/gauss.svg", MyIconUtil.class);
    }


}
