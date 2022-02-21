package com.zhang.scanner.utils;

import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;

public class MyIconUtil {
    public static final Icon SCANNER = IconLoader.getIcon("/icons/scanner.svg");

    public static final Icon LIGHTNING_DARK = IconLoader.getIcon("/icons/idea/bullet.png");

    // 参考: AllIcons.General.Tip
    public static final Icon TEXT = IconLoader.getIcon("/icons/idea/balloonInformation.png");

    public static final Icon BLOCKED = IconLoader.getIcon("/icons/degreed/blocked.svg");
    // 参考: AllIcons.General.Tip
    public static final Icon CRITICAL = IconLoader.getIcon("/icons/degreed/critical.svg");
    public static final Icon MARJOR = IconLoader.getIcon("/icons/degreed/major.svg");

    /**
     * 数据库相关图标
     */
    public static final class DataBase {
        // AllIcons.Providers.Mysql;
        public static final Icon MYSQL = IconLoader.getIcon("/icons/sqltype/mysql.svg");

        // AllIcons.Providers.Oracle;
        public static final Icon ORACLE = IconLoader.getIcon("/icons/sqltype/oracle.svg");

        // AllIcons.Providers.Postgresql;
        public static final Icon POSTGRESQL = IconLoader.getIcon("/icons/sqltype/postgresql.svg");

        // AllIcons.Providers.DB2;
        public static final Icon DB2 = IconLoader.getIcon("/icons/sqltype/DB2.svg");

        public static final Icon GAUSS = IconLoader.getIcon("/icons/sqltype/gauss.svg");
    }


}
