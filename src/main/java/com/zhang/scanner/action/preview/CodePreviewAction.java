package com.zhang.scanner.action.preview;

import com.intellij.find.FindModel;
import com.intellij.find.impl.FindInProjectUtil;
import com.intellij.ide.actions.GotoActionAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.UsageViewPresentation;
import com.intellij.usages.impl.UsagePreviewPanel;
import com.zhang.scanner.executor.TreeConsoleExecutor;
import com.zhang.scanner.pojo.ClickActionEventObject;
import com.zhang.scanner.pojo.DataKeyConst;
import com.zhang.scanner.pojo.MapperFileInfo;
import com.zhang.scanner.utils.MapperXmlHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Code Previewer Presentation 展示代码
 */
public class CodePreviewAction extends GotoActionAction {

    private static UsagePreviewPanel usagePreviewPanel = null;

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (!(e.getDataContext() instanceof SimpleDataContext)) {
            return;
        }

        SimpleDataContext dataContext = (SimpleDataContext) e.getDataContext();
        ClickActionEventObject data = dataContext.getData(DataKeyConst.CLICK_ACTION_EVENT_OBJECT_C3);
        TreeConsoleExecutor treeConsoleExecutor = data.getTreeConsoleExecutor();
        // 获取 split 面板
        Splitter splitterPanel = treeConsoleExecutor.getSplitterPanel();
        //  获取project, 等价 e.getProject()
        Project project = data.getProject();
        // 首次创建 Code Previewer 展示面板 才会初始化
        if (null == usagePreviewPanel) {
            usagePreviewPanel = intiUsagePreviewPanel(project);
        }

        Disposer.register(treeConsoleExecutor, usagePreviewPanel);
        // split 面板的右边赋值为 Code Previewer 展示面板
        splitterPanel.setSecondComponent(usagePreviewPanel);

        // 获取 mapper 文件信息
        MapperFileInfo mapperFileInfo = data.getMapperFileInfo();
        String mapperName = mapperFileInfo.getXmlPluginRuleResult().getMapperName();
        String sqlNodeIdOrg = mapperFileInfo.getXmlPluginRuleResult().getSqlNodeIdOrg();
        String filePath = mapperFileInfo.getXmlPluginRuleResult().getFilePath();

        // 返回 指定 mapper 的 PsiFile
        XmlFile xmlFile = MapperXmlHelper.findPsiFileByMapperNameAndFilePath(project, mapperName, filePath);
        XmlTag openXmlTag = null;
        if (null != xmlFile) {
            // 返回指定 mapper 的指定 sqlNodeId 位置的  XmlElement
            XmlElement xmlElement = MapperXmlHelper.findXmlElement(xmlFile, sqlNodeIdOrg);
            if (null == xmlElement) {
                // 找不到对应的 sql id ,则定位到 mapper 根节点
                openXmlTag = xmlFile.getDocument().getRootTag();
            } else {
                openXmlTag = (XmlTag) xmlElement;
            }
        }

        // 如果mapper文件为空,直接返回
        if (mapperName.isEmpty() || null == xmlFile) {
            usagePreviewPanel.updateLayout(null);
            return;
        }

        List<UsageInfo> usageInfoList = new ArrayList<>();
        XmlElement xmlElement = MapperXmlHelper.findXmlElement(xmlFile, sqlNodeIdOrg);
        if (null == xmlElement) {
            // 找不到对应的 sql id ,则定位到mapper文件首行
            usageInfoList.add(new UsageInfo(xmlFile, 0, 0));
        } else {
            XmlTag xmlTag = (XmlTag) xmlElement;
            int startOffset = xmlTag.getTextOffset();
            int endOffset = startOffset + xmlTag.getName().length();
            // startOffset:需要跳转的位置 (startOffset,endOffset) 表示一个高亮显示区间
            usageInfoList.add(new UsageInfo(xmlFile, startOffset, endOffset));
        }

        usagePreviewPanel.updateLayout(usageInfoList.isEmpty() ? null : usageInfoList);

    }

    /**
     * 初始化 Code Previewer 展示面板
     */
    public static UsagePreviewPanel intiUsagePreviewPanel(Project project) {
        // 初始化 Code Previewer Presentation
        UsageViewPresentation viewPresentation1 = FindInProjectUtil.setupViewPresentation(false, new FindModel());
        // 创建 Code Previewer 展示面板
        UsagePreviewPanel myUsagePreviewPanel = new UsagePreviewPanel(project, viewPresentation1);
        myUsagePreviewPanel.setVisible(true);
        return myUsagePreviewPanel;
    }


}
