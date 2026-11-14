package com.zhang.scanner.utils;

import com.intellij.DynamicBundle;

import java.util.Locale;

public enum LanguageUtil {
    ;
    /**
     * 当前IDEA的语言
     */
    public static boolean isChinese() {
        boolean isChinese = false;
        try {
            Locale locale = DynamicBundle.getLocale();
            Locale chinese = Locale.CHINESE;
            // 如果是中文
            if (locale.equals(chinese) || locale.getLanguage().equalsIgnoreCase(chinese.getLanguage())) {
                isChinese = true;
            }
        } catch (Exception ex) {
            // TODO
        }
        return isChinese;
    }

    public static String getLanguage() {
        Locale locale = DynamicBundle.getLocale();
        return locale.getLanguage();
    }

    /**
     * 双语文案选择: IDEA 为中文时返回中文, 其他语言一律返回英文
     *
     * @param en 英文文案(默认)
     * @param zh 中文文案
     */
    public static String zhOrEn(String en, String zh) {
        return isChinese() ? zh : en;
    }
}
