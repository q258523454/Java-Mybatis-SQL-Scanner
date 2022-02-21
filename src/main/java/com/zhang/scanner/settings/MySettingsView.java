// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.zhang.scanner.settings;

import com.intellij.util.ui.table.TableModelEditor;
import com.zhang.scanner.ui.table.rule_active_setting.MySettingRow;
import com.zhang.scanner.ui.table.rule_active_setting.MySetttingTable;

public class MySettingsView {

    /**
     * table model
     */
    private TableModelEditor<MySettingRow> tableEditor;

    public MySettingsView() {
        // 初始化 table editor 面板
        MySetttingTable mySetttingTable = new MySetttingTable();
        this.tableEditor = mySetttingTable.getTableEditor();
    }

    public TableModelEditor<MySettingRow> getTableEditor() {
        return tableEditor;
    }

    public void setTableEditor(TableModelEditor<MySettingRow> tableEditor) {
        this.tableEditor = tableEditor;
    }
}
