
package com.zhang.scanner.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.zhang.scanner.ConsoleToolRunner;
import com.zhang.scanner.executor.TreeConsoleExecutor;
import com.zhang.scanner.utils.MyExecutorUtil;
import com.zhang.zmain.parser.XmlBatisSqlParser;
import com.zhang.zmain.pojo.BaseResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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
        VirtualFile selectedDir = CommonDataKeys.VIRTUAL_FILE.getData(event.getDataContext());
        if (selectedDir == null) {
            Messages.showMessageDialog(project, "请选择文件", "文件选择错误", Messages.getInformationIcon());
            throw new RuntimeException("获取选中文件夹失败");
        }

        // 获取底部面板的执行器
        TreeConsoleExecutor executor = (TreeConsoleExecutor) MyExecutorUtil.getRunExecutorInstance(TreeConsoleExecutor.PLUGIN_ID);
        // 默认 mysql
        String dbType = executor.getDbType();

        // 构造xml解析器
        XmlBatisSqlParser xmlBatisSqlParser = new XmlBatisSqlParser();
        List<BaseResult> result = new ArrayList<>();
        try {
            // 当前所选的文件目录path
            String selectedPath = selectedDir.toString().replace("file://", "");
            // 插件直接展示到面板,无需打印日志
            result = xmlBatisSqlParser.parse(selectedPath, null, dbType);
        } catch (Exception ex) {
            throw new RuntimeException("解析异常: " + ex.getMessage());
        }

        ConsoleToolRunner consoleToolRunner = new ConsoleToolRunner(project, this, result);
        consoleToolRunner.run();
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
