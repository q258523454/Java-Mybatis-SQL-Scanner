package com.zhang.scanner.pojo;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.project.Project;
import com.zhang.scanner.executor.TreeConsoleExecutor;
import com.zhang.scanner.utils.MyLogUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 传递Action 上下文
 */
public class NodeDataContext implements DataContext {

    private Project project;

    private TreeConsoleExecutor treeConsoleExecutor;

    private MapperFileInfo mapperFileInfo;

    /**
     * 在执行下面的Action时候, 是否同时跳转到指定文件
     * {@link com.zhang.scanner.action.preview.RuleResultViewAction}
     */
    private boolean isOpenFile;

    public static final DataKey<Project> PROJECT = DataKey.create("project");

    public NodeDataContext(Project project, TreeConsoleExecutor treeConsoleExecutor, MapperFileInfo mapperFileInfo) {
        this.project = project;
        this.treeConsoleExecutor = treeConsoleExecutor;
        this.mapperFileInfo = mapperFileInfo;
    }

    public NodeDataContext(Project project, TreeConsoleExecutor treeConsoleExecutor, MapperFileInfo mapperFileInfo, boolean isOpenFile) {
        this.project = project;
        this.treeConsoleExecutor = treeConsoleExecutor;
        this.mapperFileInfo = mapperFileInfo;
        this.isOpenFile = isOpenFile;
    }

    @Nullable
    @Override
    public Object getData(@NotNull String keyName) {
        if (CommonDataKeys.PROJECT.getName().equals(keyName)) {
            return project;
        }
        MyLogUtil.error(keyName + " is not exists.");
        return null;
    }

    public MapperFileInfo getMapperFileInfo() {
        return mapperFileInfo;
    }

    public void setMapperFileInfo(MapperFileInfo mapperFileInfo) {
        this.mapperFileInfo = mapperFileInfo;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public TreeConsoleExecutor getTreeConsoleExecutor() {
        return treeConsoleExecutor;
    }

    public void setTreeConsoleExecutor(TreeConsoleExecutor treeConsoleExecutor) {
        this.treeConsoleExecutor = treeConsoleExecutor;
    }

    public boolean isOpenFile() {
        return isOpenFile;
    }

    public void setOpenFile(boolean openFile) {
        isOpenFile = openFile;
    }
}
