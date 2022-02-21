package com.zhang.scanner.pojo.tree;

import com.intellij.ui.treeStructure.SimpleNode;
import com.zhang.scanner.pojo.MapperFileInfo;
import com.zhang.scanner.utils.MyIconUtil;


public class C4DetailTreeNode extends BaseSimpleNode {

    /**
     * mapper 信息
     */
    private MapperFileInfo mapperFileInfo;

    public C4DetailTreeNode(SimpleNode aParent, MapperFileInfo mapperFileInfo) {
        // 父节点和name赋值
        super(aParent, mapperFileInfo.getParseResultShortly());
        this.mapperFileInfo = mapperFileInfo;
        this.getTemplatePresentation().setIcon(MyIconUtil.TEXT);

    }

    @Override
    protected SimpleNode[] buildChildren() {
        return new SimpleNode[0];
    }


    @Override
    public String getName() {
        return this.name;
    }

    public MapperFileInfo getMapperFileInfo() {
        return mapperFileInfo;
    }

    public void setMapperFileInfo(MapperFileInfo mapperFileInfo) {
        this.mapperFileInfo = mapperFileInfo;
    }
}
