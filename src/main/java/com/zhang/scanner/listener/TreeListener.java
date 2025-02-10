package com.zhang.scanner.listener;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.zhang.scanner.action.preview.CodePreviewAction;
import com.zhang.scanner.action.preview.RuleResultViewAction;
import com.zhang.scanner.executor.TreeConsoleExecutor;
import com.zhang.scanner.pojo.ClickActionEventObject;
import com.zhang.scanner.pojo.DataKeyConst;
import com.zhang.scanner.pojo.MapperFileInfo;
import com.zhang.scanner.pojo.tree.C3MapperFileTreeNode;
import com.zhang.scanner.pojo.tree.C4DetailTreeNode;
import com.zhang.scanner.utils.MyExecutorUtil;

import javax.swing.JPopupMenu;
import java.awt.Component;
import java.awt.event.MouseEvent;


public class TreeListener extends PopupHandler {

    @Override
    public void mouseClicked(MouseEvent e) {
        super.mouseClicked(e);
        if (!(e.getSource() instanceof SimpleTree)) {
            return;
        }
        SimpleTree selectedTree = (SimpleTree) e.getSource();
        SimpleNode selectedNode = selectedTree.getSelectedNode();

        // 双击
        if (e.getClickCount() == 2) {
            if (selectedNode instanceof C4DetailTreeNode) {
                doActionC4DetailTreeNode(e, (C4DetailTreeNode) selectedNode, true);
            }
        } else if (e.getClickCount() == 1 || e.getClickCount() > 2) {
            // 非双击
            if (selectedNode instanceof C3MapperFileTreeNode) {
                doActionC3MapperFileTreeNode(e, (C3MapperFileTreeNode) selectedNode);
            }
            if (selectedNode instanceof C4DetailTreeNode) {
                doActionC4DetailTreeNode(e, (C4DetailTreeNode) selectedNode, false);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        super.mouseReleased(e);
    }

    /**
     * 监听:弹框事件
     * 右键释放后判定为 popTrigger, 会执行 invokePopup
     */
    @Override
    public void invokePopup(Component component, int x, int y) {
        ActionGroup actionGroup = (ActionGroup) ActionManager.getInstance().getAction("zhang.group.treePop");
        // 弹框显示, 绑定 action
        JPopupMenu popup = ActionManager.getInstance().createActionPopupMenu("", actionGroup).getComponent();
        popup.show(component, x, y);
    }

    /**
     * 树形节点-第3层动作:预览代码
     */
    public void doActionC3MapperFileTreeNode(MouseEvent e, C3MapperFileTreeNode c3MapperFileTreeNode) {
        MapperFileInfo mapperFileInfo = c3MapperFileTreeNode.getMapperFileInfo();
        // 待传递内容
        TreeConsoleExecutor executor = (TreeConsoleExecutor) MyExecutorUtil.getRunExecutorInstance(TreeConsoleExecutor.PLUGIN_ID);
        ClickActionEventObject clickActionEventObject = new ClickActionEventObject();
        clickActionEventObject.setProject(executor.getProject());
        clickActionEventObject.setTreeConsoleExecutor(executor);
        clickActionEventObject.setMapperFileInfo(mapperFileInfo);
        // DataContext 不在支持继承, 通过 SimpleDataContext+DataKey 的方式来传递上下文
        DataContext dataContext = SimpleDataContext.builder()
                .add(DataKeyConst.CLICK_ACTION_EVENT_OBJECT_C3, clickActionEventObject)
                .build();

        // 创建 ActionEvent
        final ActionManager actionManager = ActionManager.getInstance();
        AnActionEvent newEvent = new AnActionEvent(e,
                dataContext,
                ActionPlaces.UNKNOWN,
                new Presentation(""),
                ActionManager.getInstance(),
                0);
        // 创建 Action 执行
        CodePreviewAction action = (CodePreviewAction) actionManager.getAction("zhang.action.CodePreviewAction");
        action.actionPerformed(newEvent);
    }

    /**
     * 树形节点-第4层动作:预览错误信息描述
     */
    public void doActionC4DetailTreeNode(MouseEvent e, C4DetailTreeNode c4DetailTreeNode, boolean isOpenFile) {
        MapperFileInfo mapperFileInfo = c4DetailTreeNode.getMapperFileInfo();

        // 待传递内容
        TreeConsoleExecutor executor = (TreeConsoleExecutor) MyExecutorUtil.getRunExecutorInstance(TreeConsoleExecutor.PLUGIN_ID);
        ClickActionEventObject clickActionEventObject = new ClickActionEventObject();
        clickActionEventObject.setProject(executor.getProject());
        clickActionEventObject.setTreeConsoleExecutor(executor);
        clickActionEventObject.setMapperFileInfo(mapperFileInfo);
        clickActionEventObject.setOpenFile(isOpenFile);
        // DataContext 不在支持继承, 通过 SimpleDataContext+DataKey 的方式来传递上下文
        DataContext dataContext = SimpleDataContext.builder()
                .add(DataKeyConst.CLICK_ACTION_EVENT_OBJECT_C4, clickActionEventObject)
                .build();

        // 创建 ActionEvent
        final ActionManager actionManager = ActionManager.getInstance();
        AnActionEvent newEvent = new AnActionEvent(e,
                dataContext,
                ActionPlaces.UNKNOWN,
                new Presentation(""),
                ActionManager.getInstance(),
                0);
        // 创建 Action 执行
        RuleResultViewAction action = (RuleResultViewAction) actionManager.getAction("zhang.action.ErrorMsgPreviewAction");
        action.actionPerformed(newEvent);
    }
}
