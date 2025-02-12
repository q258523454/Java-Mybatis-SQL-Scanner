// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.zhang.scanner.settings;

import com.zhang.zmain.enums.RuleCodeEnum;
import com.intellij.openapi.options.Configurable;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.table.TableModelEditor;
import com.zhang.scanner.ui.table.rule_active_setting.MySettingRow;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.List;
import java.util.UUID;

public class MySettingsConfig implements Configurable {

    private MySettingsView currentView;

    @Override
    public String getDisplayName() {
        return "Java Mybatis SQL Scanner Rules";
    }

    /**
     * 初始化 table panel
     * 初始化值在{@link MySettingsConfig#reset()}
     */
    @Nullable
    @Override
    public JComponent createComponent() {
        currentView = new MySettingsView();
        TableModelEditor<MySettingRow> tableEditor = currentView.getTableEditor();
        return tableEditor.createComponent();
    }

    public void init() {
        this.reset();
    }

    /**
     * 检查是否被编辑,用来触发 apply(), reset() 事件.
     */
    @Override
    public boolean isModified() {
        // 有更改
        // return currentView.getTableEditor().isModified();
        return false;
    }

    /**
     * 编辑后,点击'OK'或者'apply',保存idea窗口上的值
     */
    @Override
    public void apply() {
        MySettingsPersistent settings = MySettingsPersistent.getInstance();
        TableModelEditor<MySettingRow> tableEditor = currentView.getTableEditor();
        ListTableModel<MySettingRow> model = tableEditor.getModel();
        // 持久化到配置, 重启idea不丢失.
        settings.ruleSelectedList = model.getItems();
    }

    /**
     * 初始化重置；或者编辑后,点击'Reset'重置
     */
    @Override
    public void reset() {
        MySettingsPersistent settings = MySettingsPersistent.getInstance();
        List<MySettingRow> ruleSelectedList = settings.ruleSelectedList;
        // 1.首次初始化
        if (null == ruleSelectedList || ruleSelectedList.size() <= 0) {
            initSettings(settings);
            return;
        }
        // 2.用户点击了 新增或删除, 重新初始化.
        if (ruleSelectedList.size() != RuleCodeEnum.values().length) {
            initSettings(settings);
            return;
        }
        // 3.特殊情况: 本地存在缓存的时候,重新ruleSelectedList不为空,但是ruleCodeEnum为null
        if (ruleSelectedList.get(0).getRuleCodeEnum() == null) {
            initSettings(settings);
            return;
        }

        TableModelEditor<MySettingRow> tableEditor = currentView.getTableEditor();
        tableEditor.reset(settings.ruleSelectedList);
    }

    /**
     * 初始化用户自定义配置
     */
    private void initSettings(MySettingsPersistent settings) {
        ListTableModel<MySettingRow> model = currentView.getTableEditor().getModel();
        for (RuleCodeEnum ruleCodeEnum : RuleCodeEnum.values()) {
            // 规则等级
            MySettingRow mySettingRow = new MySettingRow(UUID.randomUUID(), ruleCodeEnum);
            model.addRow(mySettingRow);
        }
        // 持久化到配置
        settings.ruleSelectedList = model.getItems();
    }

    /**
     * 关闭Settings
     */
    @Override
    public void disposeUIResources() {
        currentView = null;
    }
}
