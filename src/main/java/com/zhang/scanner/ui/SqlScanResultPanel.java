package com.zhang.scanner.ui;


import com.intellij.psi.xml.XmlTag;
import com.intellij.util.PsiNavigateUtil;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 分屏、Tree模型、代码预览
 * 参考  {@link com.intellij.ide.todo.TodoPanel}
 */
public class SqlScanResultPanel {

    private JPanel mainPanel;

    private JTextField sqlNodeIdOrg;
    private JLabel sqlNodeIdLabel;
    private JButton openFileButton;
    private JEditorPane sqlText;
    private JEditorPane sqlScanResult;
    private JLabel sqlLabel;
    private JLabel parseLabel;
    private JTextField sqlType;

    /**
     * 用来直接跳转到指定的 xml tag 处
     * 如果找不到对应的 sql id ,则定位到 mapper 根节点
     */
    private XmlTag xmlTag;



    public JPanel getMainPanel() {
        return mainPanel;
    }

    public JTextField getSqlNodeIdOrg() {
        return sqlNodeIdOrg;
    }

    public JTextField getSqlType() {
        return sqlType;
    }

    public JEditorPane getSqlText() {
        return sqlText;
    }

    public JEditorPane getSqlScanResult() {
        return sqlScanResult;
    }

    public XmlTag getXmlTag() {
        return xmlTag;
    }

    public void setXmlTag(XmlTag xmlTag) {
        this.xmlTag = xmlTag;
    }

    public SqlScanResultPanel() {
        openFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (null != xmlTag) {
                    PsiNavigateUtil.navigate(xmlTag);
                }
            }
        });
    }
}
