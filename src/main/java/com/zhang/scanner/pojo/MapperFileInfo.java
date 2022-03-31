package com.zhang.scanner.pojo;


import com.zhang.zmain.pojo.ideaPlugin.XmlPluginRuleResult;

public class MapperFileInfo {

    private XmlPluginRuleResult xmlPluginRuleResult;


    public MapperFileInfo(XmlPluginRuleResult xmlPluginRuleResult) {
        this.xmlPluginRuleResult = xmlPluginRuleResult;
    }

    public XmlPluginRuleResult getXmlPluginRuleResult() {
        return xmlPluginRuleResult;
    }

    /**
     * 树形节点展示的时候 截断 parseResult
     */
    public String getParseResultShortly() {
        String parseResult = xmlPluginRuleResult.getParseResult();
        if (null != parseResult && !parseResult.isEmpty() && parseResult.length() > 120) {
            return parseResult.substring(0, 120) + "...";
        }
        return parseResult;
    }
}
