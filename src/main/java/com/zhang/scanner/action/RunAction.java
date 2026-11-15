
package com.zhang.scanner.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.zhang.scanner.ConsoleToolRunner;
import com.zhang.scanner.executor.TreeConsoleExecutor;
import com.zhang.scanner.utils.MyExecutorUtil;
import com.zhang.zmain.parser.XmlBatisSqlParser;
import com.zhang.zmain.pojo.BaseResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RunAction extends AnAction {

    /**
     * 保存这次 even, 用于 refresh
     */
    private AnActionEvent event;

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        myActionPerformed(anActionEvent);
    }

    public void myActionPerformed(@NotNull AnActionEvent event) {
        this.event = event;
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        VirtualFile selectedDir = CommonDataKeys.VIRTUAL_FILE.getData(event.getDataContext());
        if (selectedDir == null) {
            Messages.showMessageDialog(project, "请选择文件", "文件选择错误", Messages.getInformationIcon());
            return;
        }

        // 获取底部面板的执行器
        TreeConsoleExecutor executor = (TreeConsoleExecutor) MyExecutorUtil.getRunExecutorInstance(TreeConsoleExecutor.PLUGIN_ID);
        // 默认 mysql
        String dbType = executor.getDbType();
        // 当前所选的文件目录path (在 EDT 上取值, 后台线程只使用字符串)
        // 必须用 getPath(): toString()/getUrl() 是 URL 形式, 空格中文会变成 %20, Windows 会变成 /C:/, 扫描核心按本地路径处理会找不到文件
        String selectedPath = selectedDir.getPath();

        // 扫描(核心解析)属于耗时操作, 禁止在 EDT 上执行, 否则大目录扫描会卡死界面.
        // 解析完成后回到 EDT 创建结果面板.
        new Task.Backgroundable(project, "Java Mybatis SQL Scanner: 扫描 Mybatis SQL XML", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("正在扫描 Mybatis SQL XML...");
                try {
                    // 构造xml解析器, 后台线程执行解析
                    XmlBatisSqlParser xmlBatisSqlParser = new XmlBatisSqlParser();
                    List<BaseResult> result = xmlBatisSqlParser.parse(selectedPath, null, dbType);
                    // 结果面板必须回到 EDT 创建
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (project.isDisposed()) {
                            return;
                        }
                        ConsoleToolRunner consoleToolRunner = new ConsoleToolRunner(project, RunAction.this, result);
                        consoleToolRunner.run();
                    }, ModalityState.defaultModalityState());
                } catch (Exception ex) {
                    // 后台线程不能直接弹框, 回到 EDT 提示
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (!project.isDisposed()) {
                            Messages.showErrorDialog(project, "解析异常: " + ex.getMessage(), "扫描失败");
                        }
                    }, ModalityState.defaultModalityState());
                }
            }
        }.queue();
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


    public void rerun(Project project) {
        if (null != this.event) {
            // 设置restart (再次执行这个action)
            this.myActionPerformed(this.event);
        } else {
            Messages.showMessageDialog(project, "未选择目录!请右键待扫描目录.", "提示", Messages.getInformationIcon());
        }
    }
}
