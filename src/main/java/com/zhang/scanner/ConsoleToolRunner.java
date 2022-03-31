package com.zhang.scanner;

import com.zhang.zmain.enums.DegreeEnum;
import com.zhang.zmain.enums.RuleCodeEnum;
import com.zhang.zmain.pojo.BaseResult;
import com.zhang.zmain.pojo.ideaPlugin.XmlPluginRuleResult;
import com.zhang.zmain.pojo.ideaPlugin.XmlPluginRuleResultAll;
import com.zhang.zmain.util.ResultUtil;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import com.intellij.ui.treeStructure.Tree;
import com.zhang.scanner.action.RunAction;
import com.zhang.scanner.executor.TreeConsoleExecutor;
import com.zhang.scanner.listener.TreeListener;
import com.zhang.scanner.pojo.MapperFileInfo;
import com.zhang.scanner.pojo.tree.C1DegreeTreeNode;
import com.zhang.scanner.pojo.tree.C2RuleTreeNode;
import com.zhang.scanner.pojo.tree.C3MapperFileTreeNode;
import com.zhang.scanner.pojo.tree.C4DetailTreeNode;
import com.zhang.scanner.pojo.tree.RootTreeNode;
import com.zhang.scanner.toolbar.RefreshToolBar;
import com.zhang.scanner.toolbar.SqlTypeGroupToolBar;
import com.zhang.scanner.toolbar.TreeCollapseToolBar;
import com.zhang.scanner.toolbar.TreeExpandToolBar;
import com.zhang.scanner.ui.table.operation.MyOperationTable;
import com.zhang.scanner.utils.MyExecutorUtil;
import com.zhang.scanner.utils.MyIconUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsoleToolRunner implements Disposable {

    /**
     * Project
     */
    private Project project;

    private RunAction runAction;

    /**
     * 扫描结果
     */
    List<BaseResult> scanResult = new ArrayList<>();


    public ConsoleToolRunner(@NotNull Project project, @NotNull RunAction runAction, List<BaseResult> scanResult) {
        this.project = project;
        this.runAction = runAction;
        this.scanResult = scanResult;
    }


    public void run() {
        // 获取 Executor bean
        TreeConsoleExecutor executor = (TreeConsoleExecutor) MyExecutorUtil.getRunExecutorInstance(TreeConsoleExecutor.PLUGIN_ID);
        if (executor == null) {
            return;
        }

        // 创建 tree
        Tree myTree = null;
        if (null != scanResult && !scanResult.isEmpty()) {
            myTree = createSimpleTree();
        }
        executor.setTree(myTree);
        executor.setProject(project);


        // 创建 RunnerLayoutUi
        final RunnerLayoutUi.Factory factory = RunnerLayoutUi.Factory.getInstance(project);
        RunnerLayoutUi layoutUi = factory.create("id", "title", "session name", project);

        // 创建 ToolWindow 左右分屏窗口
        Splitter splitterPanel = new OnePixelSplitter(false);
        splitterPanel.setFirstComponent(ScrollPaneFactory.createScrollPane(myTree));
        splitterPanel.setSecondComponent(null);
        executor.setSplitterPanel(splitterPanel);

        // 创建描述信息
        RunContentDescriptor descriptor = createDescriptor(layoutUi);
        descriptor.setExecutionId(System.nanoTime());

        // 新增第1个content, 展示 扫描结果
        final Content content = layoutUi.createContent("contentId", splitterPanel,
                "SQL Scanner", AllIcons.Debugger.Console, splitterPanel);
        // 不允许关闭
        content.setCloseable(false);
        layoutUi.addContent(content);

        MyOperationTable myOperationTable = new MyOperationTable();
        myOperationTable.init(scanResult);
        // 新增第2个content, 展示 操作类型 汇总
        final Content content2 = layoutUi.createContent("contentId", myOperationTable.getTable(),
                "Table Operation", AllIcons.Debugger.Console, myOperationTable.getTable());
        // 不允许关闭
        content.setCloseable(false);
        layoutUi.addContent(content2);


        // 新增左边工具条
        layoutUi.getOptions().setLeftToolbar(createActionToolbar(myTree), "Toolbar");
        ExecutionManager.getInstance(project).getContentManager().showRunContent(executor, descriptor);
    }

    private ActionGroup createActionToolbar(Tree tree) {
        final DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new RefreshToolBar(project,runAction));
        actionGroup.add(new TreeExpandToolBar(tree));
        actionGroup.add(new TreeCollapseToolBar(tree));
        actionGroup.add(new SqlTypeGroupToolBar());
        return actionGroup;
    }

    /**
     * 分屏、Tree模型、代码预览
     * 参考  {@link com.intellij.ide.todo.TodoPanel}
     */
    private Tree createSimpleTree() {
        // 自定义规则解析扫描结果
        XmlPluginRuleResultAll xmlPluginRuleResultAll = ResultUtil.doRuleAll(scanResult);
        Map<RuleCodeEnum, List<XmlPluginRuleResult>> ruleMap = xmlPluginRuleResultAll.getRuleMap();

        // 创建root主节点
        RootTreeNode root = new RootTreeNode("Mybatis Scanner");

        // 创建1级子节点(阻塞,严重,主要),固定顺序
        Map<String, C1DegreeTreeNode> c1DegreeTreeNodeMap = new HashMap<>();
        for (DegreeEnum degreeEnum : DegreeEnum.values()) {
            String code = degreeEnum.getCode();
            if (null == c1DegreeTreeNodeMap.get(code)) {
                c1DegreeTreeNodeMap.put(code, new C1DegreeTreeNode(root, code));
                C1DegreeTreeNode newC1DegreeTreeNode = c1DegreeTreeNodeMap.get(code);
                root.add(newC1DegreeTreeNode);
            }
        }
        for (Map.Entry<RuleCodeEnum, List<XmlPluginRuleResult>> entry : ruleMap.entrySet()) {
            RuleCodeEnum ruleCodeEnum = entry.getKey();
            String degree = ruleCodeEnum.getDegreeEnum().getCode();
            // 根据规则rule code, 创建2级子节点
            C1DegreeTreeNode c1 = c1DegreeTreeNodeMap.get(degree);
            C2RuleTreeNode c2 = new C2RuleTreeNode(c1, ruleCodeEnum);
            c1.add(c2);

            // 当前 rule 下的所有 list
            List<XmlPluginRuleResult> ruleList = entry.getValue();
            for (XmlPluginRuleResult xmlPluginRuleResult : ruleList) {
                MapperFileInfo mapperFileInfo = new MapperFileInfo(xmlPluginRuleResult);
                // 创建3级子节点
                C3MapperFileTreeNode c3 = new C3MapperFileTreeNode(c2, mapperFileInfo);
                // 添加到2级子节点下
                c2.add(c3);
                // 创建4级子节点
                C4DetailTreeNode c4 = new C4DetailTreeNode(c3, mapperFileInfo);
                // 添加到3级子节点下
                c3.add(c4);
            }
        }
        // 设置 tree structure
        SimpleTreeStructure treeStructure = new SimpleTreeStructure.Impl(root);
        // 设置 tree root 节点的结构 structure
        StructureTreeModel<SimpleTreeStructure> structureTreeModel = new StructureTreeModel<>(treeStructure, this);
        // structure 保存到 root 节点
        root.setStructureTreeModel(structureTreeModel);
        // 设置 tree 节点的 model
        AsyncTreeModel asyncTreeModel = new AsyncTreeModel(structureTreeModel, this);
        // 创建树状模型
        Tree simpleTree = new SimpleTree();
        // tree model
        simpleTree.setModel(asyncTreeModel);
        // 绑定监听器
        simpleTree.addMouseListener(new TreeListener());
        // simpleTree.addTreeSelectionListener();
        return simpleTree;
    }

    /**
     * 根据  layoutUi创建描述信息
     */
    public RunContentDescriptor createDescriptor(RunnerLayoutUi layoutUi) {
        // 创建描述信息
        RunContentDescriptor descriptor = new RunContentDescriptor(new RunProfile() {
            @Nullable
            @Override
            public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException {
                return null;
            }

            @NotNull
            @Override
            public String getName() {
                return "Scanner";
            }

            @Override
            public Icon getIcon() {
                return MyIconUtil.SCANNER;
            }

        }, new DefaultExecutionResult(), layoutUi);

        return descriptor;
    }

    @Override
    public void dispose() {
        Disposer.dispose(this);
    }
}
