package com.zhang.scanner.pojo.tree;

import com.intellij.ui.treeStructure.SimpleNode;
import com.zhang.scanner.pojo.MapperFileInfo;
import com.zhang.scanner.utils.MyIconUtil;

import java.util.ArrayList;
import java.util.List;


public class C3MapperFileTreeNode extends BaseSimpleNode {

    /**
     * mapper 信息
     */
    private MapperFileInfo mapperFileInfo;

    /**
     * child tree
     */
    private List<C4DetailTreeNode> c4DetailTreeNodeList = new ArrayList<>();


    public C3MapperFileTreeNode(SimpleNode aParent, MapperFileInfo mapperFileInfo) {
        // 父节点和name赋值
        super(aParent, mapperFileInfo.getXmlPluginRuleResult().getMapperName() +
                " (id:" + mapperFileInfo.getXmlPluginRuleResult().getSqlNodeIdOrg() + ")");
        this.mapperFileInfo = mapperFileInfo;
        this.getTemplatePresentation().setIcon(MyIconUtil.LIGHTNING_DARK);
    }

    @Override
    protected SimpleNode[] buildChildren() {
        int size = c4DetailTreeNodeList.size();
        return c4DetailTreeNodeList.toArray(new C4DetailTreeNode[size]);
    }


    @Override
    public String getName() {
        return this.name;
    }

    public void add(C4DetailTreeNode c4DetailTreeNode) {
        checkChild(this, c4DetailTreeNode);
        this.c4DetailTreeNodeList.add(c4DetailTreeNode);
    }

    public void add(int index, C4DetailTreeNode c4DetailTreeNode) {
        checkChild(this, c4DetailTreeNode);
        this.c4DetailTreeNodeList.add(index, c4DetailTreeNode);
    }

    public MapperFileInfo getMapperFileInfo() {
        return mapperFileInfo;
    }

    public void setMapperFileInfo(MapperFileInfo mapperFileInfo) {
        this.mapperFileInfo = mapperFileInfo;
    }
}
