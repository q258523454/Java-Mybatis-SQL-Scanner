package com.zhang.scanner.pojo.tree;

import com.intellij.ui.treeStructure.SimpleNode;

import java.util.ArrayList;
import java.util.List;

public class C1DegreeTreeNode extends BaseSimpleNode {


    /**
     * child tree
     */
    private List<C2RuleTreeNode> c2RuleTreeNodeList = new ArrayList<>();


    public C1DegreeTreeNode(SimpleNode aParent, String name) {
        // 父节点和name赋值
        super(aParent, name);
    }

    @Override
    protected SimpleNode[] buildChildren() {
        int size = c2RuleTreeNodeList.size();
        return c2RuleTreeNodeList.toArray(new C2RuleTreeNode[size]);
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void add(C2RuleTreeNode c2RuleTreeNode) {
        checkChild(this, c2RuleTreeNode);
        this.c2RuleTreeNodeList.add(c2RuleTreeNode);
    }

    public void add(int index, C2RuleTreeNode c2RuleTreeNode) {
        checkChild(this, c2RuleTreeNode);
        this.c2RuleTreeNodeList.add(index, c2RuleTreeNode);
    }

}
