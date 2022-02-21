package com.zhang.scanner.pojo.tree;

import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.treeStructure.CachingSimpleNode;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTreeStructure;

import java.util.ArrayList;
import java.util.List;

public class RootTreeNode extends BaseSimpleNode {


    /**
     * tree child
     */
    private List<C1DegreeTreeNode> c1DegreeTreeNodeList = new ArrayList<>();

    /**
     * tree structure
     */
    public StructureTreeModel<SimpleTreeStructure> structureTreeModel;

    public RootTreeNode(String name) {
        super(null, name);
    }

    /**
     * 每次展示当前 tree 层级的时候执行,折叠的时候不会执行
     */
    @Override
    protected SimpleNode[] buildChildren() {
        int size = c1DegreeTreeNodeList.size();
        return c1DegreeTreeNodeList.toArray(new C1DegreeTreeNode[size]);
    }

    @Override
    public String getName() {
        return this.name;
    }


    public void add(C1DegreeTreeNode c1DegreeTreeNode) {
        checkChild(this, c1DegreeTreeNode);
        this.c1DegreeTreeNodeList.add(c1DegreeTreeNode);
    }

    public void add(int index, C1DegreeTreeNode c1DegreeTreeNode) {
        checkChild(this, c1DegreeTreeNode);
        this.c1DegreeTreeNodeList.add(index, c1DegreeTreeNode);
    }


    public void updateTree(CachingSimpleNode root) {
        if (root != null) {
            // children = null
            root.cleanUpCache();
            // true 表示刷新整个 tree 节点, 会执行所有 tree(包括children) 的 getName()
            structureTreeModel.invalidate(root, true);
        }
    }

    public void setStructureTreeModel(StructureTreeModel<SimpleTreeStructure> structureTreeModel) {
        this.structureTreeModel = structureTreeModel;
    }
}
