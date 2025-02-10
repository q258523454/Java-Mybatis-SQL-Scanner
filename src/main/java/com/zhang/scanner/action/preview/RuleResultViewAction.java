package com.zhang.scanner.action.preview;

import com.intellij.ide.actions.GotoActionAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.PsiNavigateUtil;
import com.zhang.scanner.executor.TreeConsoleExecutor;
import com.zhang.scanner.pojo.ClickActionEventObject;
import com.zhang.scanner.pojo.DataKeyConst;
import com.zhang.scanner.pojo.MapperFileInfo;
import com.zhang.scanner.ui.SqlScanResultPanel;
import com.zhang.scanner.utils.MapperXmlHelper;

/**
 * 展示扫描结果
 */
public class RuleResultViewAction extends GotoActionAction {


    private static SqlScanResultPanel sqlScanResultPanel = null;

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (!(e.getDataContext() instanceof SimpleDataContext)) {
            return;
        }

        XmlTag xmlTag = null;
        SimpleDataContext dataContext = (SimpleDataContext) e.getDataContext();
        //  获取project, 等价 e.getProject()
        ClickActionEventObject data = dataContext.getData(DataKeyConst.CLICK_ACTION_EVENT_OBJECT_C4);
        Project project = data.getProject();

        // 获取 mapper 文件信息
        MapperFileInfo mapperFileInfo = data.getMapperFileInfo();
        String mapperName = mapperFileInfo.getXmlPluginRuleResult().getMapperName();
        String sqlNodeIdOrg = mapperFileInfo.getXmlPluginRuleResult().getSqlNodeIdOrg();
        String filePath = mapperFileInfo.getXmlPluginRuleResult().getFilePath();
        // 返回 指定 mapper 的 PsiFile
        XmlFile xmlFile = MapperXmlHelper.findPsiFileByMapperNameAndFilePath(project, mapperName, filePath);
        if (null != xmlFile) {
            // 返回指定 mapper 的指定 sqlNodeId 位置的  XmlElement
            XmlElement xmlElement = MapperXmlHelper.findXmlElement(xmlFile, sqlNodeIdOrg);
            if (null == xmlElement) {
                // 找不到对应的 sql id ,则定位到 mapper 根节点
                xmlTag = xmlFile.getDocument().getRootTag();
            } else {
                xmlTag = (XmlTag) xmlElement;
            }
        }

        String dbType = mapperFileInfo.getXmlPluginRuleResult().getDbType();
        // 返回SQL, 优先返回有格式化的SQL
        String sqlText = mapperFileInfo.getXmlPluginRuleResult().getSqlStr();
        String parseResult = mapperFileInfo.getXmlPluginRuleResult().getParseResult();

        // 首次才会new
        if (null == sqlScanResultPanel) {
            sqlScanResultPanel = new SqlScanResultPanel();
        }
        sqlScanResultPanel.getSqlNodeIdOrg().setText(sqlNodeIdOrg);
        sqlScanResultPanel.getSqlType().setText(dbType);
        sqlScanResultPanel.getSqlText().setText(sqlText);
        sqlScanResultPanel.getSqlScanResult().setText(parseResult);
        sqlScanResultPanel.setXmlTag(xmlTag);

        TreeConsoleExecutor treeConsoleExecutor = data.getTreeConsoleExecutor();
        Splitter splitterPanel = treeConsoleExecutor.getSplitterPanel();
        splitterPanel.setSecondComponent(sqlScanResultPanel.getMainPanel());

        // 是否同时跳转到指定文件.
        if (data.isOpenFile()) {
            if (null != xmlTag) {
                PsiNavigateUtil.navigate(xmlTag);
            }
        }
    }

}
