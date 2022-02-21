package com.zhang.scanner.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.zhang.scanner.executor.TreeConsoleExecutor;
import com.zhang.scanner.pojo.tree.BaseSimpleNode;
import com.zhang.scanner.utils.MyExecutorUtil;
import com.zhang.scanner.utils.MyLogUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreePath;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

public class CopyTreeSelectedAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (null != e.getProject()) {
            Project project = e.getProject();
            TreeConsoleExecutor executor = (TreeConsoleExecutor) MyExecutorUtil.getRunExecutorInstance(TreeConsoleExecutor.PLUGIN_ID);
            if (null != executor) {
                SimpleTree tree = (SimpleTree) executor.getTree();
                // 获取当前 tree, 所选择的 items
                List<SimpleNode> selectedNodes = getSelectedNodes(tree);

                StringBuilder result = new StringBuilder("");
                for (SimpleNode node : selectedNodes) {
                    // 只要选择 tree item 不是指定类型,直接跳过.
                    if (!(node instanceof BaseSimpleNode)) {
                        continue;
                    }
                    result.append(node.getName()).append("\n");
                }

                if (!result.toString().isEmpty()) {
                    CopyPasteManager.getInstance().setContents(new StringSelection(result.toString()));
                    MyLogUtil.info("Copied to pasteboard success");
                } else {
                    MyLogUtil.error("Copied to pasteboard fail");
                }
            }
        }
    }


    /**
     * 获取当前 tree, 所选择的 items
     */
    private static List<SimpleNode> getSelectedNodes(SimpleTree tree) {
        List<SimpleNode> list = new ArrayList<>();
        // 获取所选 items
        TreePath[] selectionPaths = tree.getSelectionPaths();
        if (null != selectionPaths) {
            for (TreePath treePath : selectionPaths) {
                SimpleNode nodeFor = tree.getNodeFor(treePath);
                list.add(nodeFor);
            }
        }
        return list;
    }

}
