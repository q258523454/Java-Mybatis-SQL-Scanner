package com.zhang.scanner.toolbar;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.zhang.scanner.action.RunAction;

/**
 * 刷新, 再次执行
 * {@link RunAction}
 */
public class RefreshToolBar extends AnAction implements DumbAware {

    private Project project;
    private RunAction action;

    public RefreshToolBar(Project project, RunAction action) {
        super("Rerun", "Rerun", AllIcons.Actions.Rerun);
        this.project = project;
        this.action = action;
    }

    /**
     * 执行动作
     */
    @Override
    public void actionPerformed(AnActionEvent e) {
        // 设置restart (再次执行这个action)
        Runnable rerun = () -> action.rerun(project);
        rerun.run();
    }

    /**
     * 监听该Action, 每次焦点更换、窗口切换都会触发 update
     */
    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setVisible(e.getProject() != null);
    }
}
