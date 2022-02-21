package com.zhang.scanner.utils;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;


public class MyLogUtil {

    public static void error(String content) {
        Notifications.Bus.notify(new Notification("", "", content, NotificationType.ERROR));
    }

    public static void info(String content) {
        Notifications.Bus.notify(new Notification("", "", content, NotificationType.INFORMATION));
    }
}

