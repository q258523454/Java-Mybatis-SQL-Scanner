// Copyright 2000-2021 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.zhang.scanner.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.zhang.scanner.ui.table.rule_active_setting.MySettingRow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * idea配置持久化
 */
@State(
        name = "MySettingsPersistent",
        storages = @Storage("MySettingsPersistent.xml")
)
public class MySettingsPersistent implements PersistentStateComponent<MySettingsPersistent> {


    /**
     * 因为要序列化,必须是 public
     */
    public List<MySettingRow> ruleSelectedList;

    public static MySettingsPersistent getInstance() {
        return ApplicationManager.getApplication().getComponent(MySettingsPersistent.class);
    }

    @Nullable
    @Override
    public MySettingsPersistent getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull MySettingsPersistent state) {
        XmlSerializerUtil.copyBean(state, this);
    }

}
