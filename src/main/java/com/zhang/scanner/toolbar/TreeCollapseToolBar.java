package com.zhang.scanner.toolbar;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;

public class TreeCollapseToolBar extends AnAction {

    private Tree tree;

    public TreeCollapseToolBar(Tree tree) {
        super("Collapse All", "Collapse all", AllIcons.Actions.Collapseall);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // int rowCount = tree.getRowCount();
        // for (int i = 0; i <= rowCount; i++) {
        //     tree.collapseRow(i);
        // }
        TreeUtil.collapseAll(tree, 0);
    }
}
