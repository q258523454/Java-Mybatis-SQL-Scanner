package com.zhang.scanner.pojo;

import com.intellij.openapi.project.Project;
import com.zhang.scanner.executor.TreeConsoleExecutor;

public class ClickActionEventObject {
    private Project project;

    private TreeConsoleExecutor treeConsoleExecutor;

    private MapperFileInfo mapperFileInfo;

    /**
     * 在执行下面的Action时候, 是否同时跳转到指定文件
     * {@link com.zhang.scanner.action.preview.RuleResultViewAction}
     */
    private boolean isOpenFile;

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

    public MapperFileInfo getMapperFileInfo() {
        return mapperFileInfo;
    }

    public void setMapperFileInfo(MapperFileInfo mapperFileInfo) {
        this.mapperFileInfo = mapperFileInfo;
    }

    public boolean isOpenFile() {
        return isOpenFile;
    }

    public void setOpenFile(boolean openFile) {
        isOpenFile = openFile;
    }
}
