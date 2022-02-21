package com.zhang.scanner.ui.table.rule_active_setting;

import com.intellij.openapi.util.Iconable;
import com.zhang.scanner.utils.MyIconUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public enum MyRuleIcon implements Iconable {
    /**
     * Blocked
     */
    BLOCKED("Blocked", MyIconUtil.BLOCKED),
    CRITICAL("Critical", MyIconUtil.CRITICAL),
    MAJOR("Major", MyIconUtil.MARJOR);

    private final String myName;
    private final Icon myIcon;

    MyRuleIcon(@NotNull String name,
               @NotNull Icon icon) {
        myName = name;
        myIcon = icon;
    }


    public String getName() {
        return myName;
    }

    public Icon getIcon() {
        return myIcon;
    }

    @Override
    public String toString() {
        return myName;
    }

    @Override
    public Icon getIcon(@IconFlags int flags) {
        return getIcon();
    }
}