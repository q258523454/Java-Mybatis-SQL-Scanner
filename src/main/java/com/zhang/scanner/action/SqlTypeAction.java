// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.zhang.scanner.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.zhang.scanner.enums.SqlTypeEnum;
import com.zhang.scanner.executor.TreeConsoleExecutor;
import com.zhang.scanner.utils.MyExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class SqlTypeAction extends AnAction {

    private String sqlType;

    public SqlTypeAction(@Nullable String sqlType, @Nullable String description, @Nullable Icon icon) {
        super(sqlType, description, icon);
        this.sqlType = sqlType;
    }


    /**
     * 针对不同类型的 SQl, 做不同的执行
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        // 获取底部面板的执行器
        TreeConsoleExecutor executor = (TreeConsoleExecutor) MyExecutorUtil.getRunExecutorInstance(TreeConsoleExecutor.PLUGIN_ID);

        if (SqlTypeEnum.ORACLE.getCode().equalsIgnoreCase(sqlType)) {
            // TODO
        }

        if (SqlTypeEnum.MYSQL.getCode().equalsIgnoreCase(sqlType)) {
            // TODO
        }

        if (SqlTypeEnum.GAUSS.getCode().equalsIgnoreCase(sqlType)) {
            // TODO
        }

        if (SqlTypeEnum.DB2.getCode().equalsIgnoreCase(sqlType)) {
            // TODO
        }

        if (SqlTypeEnum.POSTGRESQL.getCode().equalsIgnoreCase(sqlType)) {
            // TODO
        }

        executor.setDbType(sqlType);
        Project project = event.getProject();
        Messages.showMessageDialog(project, "SQL Type Change Sucess:" + sqlType, "SQL Type", Messages.getInformationIcon());
    }

    /**
     * 监听该Action, 每次焦点更换、窗口切换都会触发 update
     * 只要项目打开,这个按钮就一直显示可用
     */
    @Override
    public void update(AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }

}
