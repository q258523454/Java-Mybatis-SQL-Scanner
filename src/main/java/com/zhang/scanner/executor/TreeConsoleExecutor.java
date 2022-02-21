package com.zhang.scanner.executor;

import com.intellij.execution.Executor;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.treeStructure.Tree;
import com.zhang.scanner.utils.MyIconUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * 在plugin.xml注册,启动就会注入成一个 bean
 * 通过 ExecutorRegistry.getInstance().getExecutorById() 获取该实例
 */
public class TreeConsoleExecutor extends Executor implements Disposable {


    public static final String PLUGIN_ID = "SQL Plugin ID";

    public static final String TOOL_WINDOW_ID = "Scanner";


    /**
     * project 属性
     */
    private Project project;

    /**
     * tree
     */
    private Tree tree;

    /**
     * split 面板, 底部左右分屏窗口
     */
    private Splitter splitterPanel = new OnePixelSplitter(false);

    /**
     * 保存数据库类型(默认: mysql)
     */
    private String dbType = "mysql";


    @NotNull
    @Override
    public String getId() {
        return PLUGIN_ID;
    }

    @Override
    public String getToolWindowId() {
        return TOOL_WINDOW_ID;
    }

    @Override
    public Icon getToolWindowIcon() {
        return MyIconUtil.SCANNER;
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return MyIconUtil.SCANNER;
    }

    @Override
    public Icon getDisabledIcon() {
        return MyIconUtil.SCANNER;
    }

    @Override
    public String getDescription() {
        return TOOL_WINDOW_ID;
    }

    @NotNull
    @Override
    public String getActionName() {
        return TOOL_WINDOW_ID;
    }

    @NotNull
    @Override
    public String getStartActionText() {
        return TOOL_WINDOW_ID;
    }

    @Override
    public String getContextActionId() {
        return "custom context action id";
    }

    @Override
    public String getHelpId() {
        return TOOL_WINDOW_ID;
    }

    public Tree getTree() {
        return tree;
    }

    public void setTree(Tree tree) {
        this.tree = tree;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }


    public Splitter getSplitterPanel() {
        return splitterPanel;
    }

    public void setSplitterPanel(Splitter splitterPanel) {
        this.splitterPanel = splitterPanel;
    }

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    @Override
    public void dispose() {
        Disposer.dispose(this);
    }
}

