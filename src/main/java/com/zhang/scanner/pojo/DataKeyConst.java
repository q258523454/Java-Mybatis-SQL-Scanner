package com.zhang.scanner.pojo;

import com.intellij.openapi.actionSystem.DataKey;

public enum DataKeyConst {
    ;
    /**
     * com.intellij.openapi.actionSystem.DataContext 不在支持继承, 通过 SimpleDataContext+DataKey 的方式来传递上下文
     */
    public static final DataKey<ClickActionEventObject> CLICK_ACTION_EVENT_OBJECT_C3 = DataKey.create("clickActionEventObjectC3");

    public static final DataKey<ClickActionEventObject> CLICK_ACTION_EVENT_OBJECT_C4 = DataKey.create("clickActionEventObjectC4");
}
