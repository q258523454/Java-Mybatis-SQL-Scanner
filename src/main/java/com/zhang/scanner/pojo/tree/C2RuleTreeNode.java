package com.zhang.scanner.pojo.tree;

import com.cmb.zmain.enums.DegreeEnum;
import com.cmb.zmain.enums.RuleCodeEnum;
import com.intellij.ui.treeStructure.SimpleNode;
import com.zhang.scanner.utils.MyIconUtil;

import java.util.ArrayList;
import java.util.List;


public class C2RuleTreeNode extends BaseSimpleNode {

    private RuleCodeEnum ruleCodeEnum;

    /**
     * child tree
     */
    private List<C3MapperFileTreeNode> c3MapperFileTreeNodeList = new ArrayList<>();


    public C2RuleTreeNode(SimpleNode aParent, RuleCodeEnum ruleCodeEnum) {
        // 父节点和name赋值
        super(aParent, ruleCodeEnum.name() + ":" + ruleCodeEnum.getDesc());
        this.ruleCodeEnum = ruleCodeEnum;
        if (null != ruleCodeEnum.getDegreeEnum()) {
            DegreeEnum degreeEnum = ruleCodeEnum.getDegreeEnum();
            if (degreeEnum == DegreeEnum.BLOCKED) {
                this.getTemplatePresentation().setIcon(MyIconUtil.BLOCKED);
            }
            if (degreeEnum == DegreeEnum.CRITICAL) {
                this.getTemplatePresentation().setIcon(MyIconUtil.CRITICAL);
            }
            if (degreeEnum == DegreeEnum.MAJOR) {
                this.getTemplatePresentation().setIcon(MyIconUtil.MARJOR);
            }
        }
    }

    @Override
    protected SimpleNode[] buildChildren() {
        int size = c3MapperFileTreeNodeList.size();
        return c3MapperFileTreeNodeList.toArray(new C3MapperFileTreeNode[size]);
    }


    @Override
    public String getName() {
        return this.name;
    }

    public void add(C3MapperFileTreeNode c3MapperFileTreeNode) {
        checkChild(this, c3MapperFileTreeNode);
        this.c3MapperFileTreeNodeList.add(c3MapperFileTreeNode);
    }

    public void add(int index, C3MapperFileTreeNode c3MapperFileTreeNode) {
        checkChild(this, c3MapperFileTreeNode);
        this.c3MapperFileTreeNodeList.add(index, c3MapperFileTreeNode);
    }


    public RuleCodeEnum getRuleCodeEnum() {
        return ruleCodeEnum;
    }

    public void setRuleCodeEnum(RuleCodeEnum ruleCodeEnum) {
        this.ruleCodeEnum = ruleCodeEnum;
    }
}
