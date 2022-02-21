package com.zhang.scanner.toolbar;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;

public class TreeExpandToolBar extends AnAction {

    private Tree tree;

    public TreeExpandToolBar(Tree tree) {
        super("Expand All", "Expand all", AllIcons.Actions.Expandall);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // int rowCount = tree.getRowCount();
        // for (int i = 0; i <= rowCount; i++) {
        //     tree.expandRow(i);
        // }
        TreeUtil.expandAll(tree);
    }
}
