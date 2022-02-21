package com.zhang.scanner.utils;

import com.intellij.openapi.application.ApplicationManager;

public enum MyComponentUtil {
    ;

    /**
     * 根据Class获取bean
     */
    public static <T> T getInstance(Class<T> cls) {
        return ApplicationManager.getApplication().getComponent(cls);
    }
}
