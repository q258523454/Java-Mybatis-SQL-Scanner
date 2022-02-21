package com.zhang.scanner.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SqlTypeActionGroup extends DefaultActionGroup {


    /**
     * Group Action 容器
     */
    public SqlTypeActionGroup(@NotNull List<? extends AnAction> actions) {
        // 添加标题分隔
        this.addSeparator("SQL Type");
        // 添加所有的 action
        this.addAll(actions);
        // 设置图标
        this.getTemplatePresentation().setIcon(AllIcons.Actions.GroupBy);
        // 弹出
        this.setPopup(true);
    }

    public void actionPerformed(@NotNull AnActionEvent e) {
        JBPopupFactory.getInstance().createActionGroupPopup(null, this, e.getDataContext(), JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true).showUnderneathOf(e.getInputEvent().getComponent());
    }


}
