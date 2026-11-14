package com.zhang.scanner.ai;

/**
 * Ask AI 提示词模板(固定结构)
 * 语言由调用方传入的 AI 回答语言偏好决定, 与 UI 国际化无关
 */
public final class AiPromptTemplate {

    private AiPromptTemplate() {
    }

    /**
     * 系统提示词(角色定位)
     *
     * @param chinese AI 回答语言是否为中文(由用户的 AI 语言偏好决定, 与 UI 语言无关)
     */
    public static String buildSystemPrompt(boolean chinese) {
        if (chinese) {
            return "你是一名资深数据库专家与 SQL 优化顾问，擅长 MySQL、Oracle、DB2、PostgreSQL 等主流数据库的性能分析与 SQL 调优。"
                    + "请根据用户提供的 SQL 上下文，指出 SQL 存在的问题、风险与可优化之处，并给出具体、可执行的优化建议。"
                    + "回答请使用中文。";
        }
        return "You are a senior database expert and SQL optimization consultant, proficient in performance analysis "
                + "and SQL tuning for mainstream databases such as MySQL, Oracle, DB2, and PostgreSQL. "
                + "Based on the SQL context provided by the user, identify problems, risks, and optimization "
                + "opportunities in the SQL, and give specific, actionable suggestions. "
                + "Please answer in English.";
    }

    /**
     * 用户提示词(固定模板: 四段上下文 + 固定三段式输出格式)
     *
     * @param chinese     AI 回答语言是否为中文(由用户的 AI 语言偏好决定, 与 UI 语言无关)
     * @param sqlType     SQL Type(数据库类型)
     * @param rawXml      解析前的原始 XML 语句(空时显示占位符)
     * @param sqlText     格式化后的 SQL
     * @param parseResult 报错信息或建议(空时显示占位符)
     */
    public static String buildUserPrompt(boolean chinese, String sqlType, String rawXml, String sqlText, String parseResult) {
        if (chinese) {
            return buildUserPromptChinese(sqlType, rawXml, sqlText, parseResult);
        }
        return buildUserPromptEnglish(sqlType, rawXml, sqlText, parseResult);
    }

    /**
     * 中文版用户提示词
     */
    private static String buildUserPromptChinese(String sqlType, String rawXml, String sqlText, String parseResult) {
        String rawXmlText = (null == rawXml || rawXml.trim().isEmpty()) ? "（无）" : rawXml;
        String suggestion = (null == parseResult || parseResult.trim().isEmpty()) ? "（无）" : parseResult;
        StringBuilder sb = new StringBuilder();
        sb.append("请分析以下 MyBatis 映射文件中的 SQL（该 SQL 由 MyBatis XML 解析而来）：\n\n");
        sb.append("【SQL Type】\n").append(sqlType).append("\n\n");
        sb.append("【SQL XML】\n").append(rawXmlText).append("\n\n");
        sb.append("【格式化后的 SQL】\n");
        sb.append("```sql\n").append(sqlText).append("\n```\n\n");
        sb.append("【报错信息或建议】\n").append(suggestion).append("\n\n");
        sb.append("请从正确性、性能、索引使用、潜在风险等角度分析这条 SQL，")
                .append("并严格按照以下 Markdown 模板输出分析报告，不要输出模板之外的任何内容：\n\n");
        sb.append("# SQL 分析报告\n\n");
        sb.append("## 1. SQL语句\n\n");
        sb.append("```sql\n");
        sb.append("（原样输出上面的 SQL）\n");
        sb.append("```\n\n");
        sb.append("## 2. 潜在风险\n\n");
        sb.append("1. **风险标题**：风险说明\n");
        sb.append("2. **风险标题**：风险说明\n");
        sb.append("（按实际发现的风险数量输出多条，没有风险则输出\"无\"）\n\n");
        sb.append("## 3. 修改建议\n\n");
        sb.append("1. **建议标题**：具体可执行的修改建议\n");
        sb.append("2. **建议标题**：具体可执行的修改建议\n");
        sb.append("（按实际建议数量输出多条）");
        return sb.toString();
    }

    /**
     * 英文版用户提示词
     */
    private static String buildUserPromptEnglish(String sqlType, String rawXml, String sqlText, String parseResult) {
        String rawXmlText = (null == rawXml || rawXml.trim().isEmpty()) ? "(None)" : rawXml;
        String suggestion = (null == parseResult || parseResult.trim().isEmpty()) ? "(None)" : parseResult;
        StringBuilder sb = new StringBuilder();
        sb.append("Please analyze the SQL in the following MyBatis mapper (the SQL is parsed from MyBatis XML):\n\n");
        sb.append("【SQL Type】\n").append(sqlType).append("\n\n");
        sb.append("【SQL XML】\n").append(rawXmlText).append("\n\n");
        sb.append("【Formatted SQL】\n");
        sb.append("```sql\n").append(sqlText).append("\n```\n\n");
        sb.append("【Errors/Suggestions】\n").append(suggestion).append("\n\n");
        sb.append("Analyze this SQL from the perspectives of correctness, performance, index usage, and potential risks, ")
                .append("and output the analysis report strictly in the following Markdown template. ")
                .append("Do not output anything outside the template:\n\n");
        sb.append("# SQL Analysis Report\n\n");
        sb.append("## 1. SQL Statement\n\n");
        sb.append("```sql\n");
        sb.append("(output the SQL above as-is)\n");
        sb.append("```\n\n");
        sb.append("## 2. Potential Risks\n\n");
        sb.append("1. **Risk title**: risk description\n");
        sb.append("2. **Risk title**: risk description\n");
        sb.append("(output as many items as actually found; output \"None\" if there are no risks)\n\n");
        sb.append("## 3. Recommendations\n\n");
        sb.append("1. **Suggestion title**: specific actionable recommendation\n");
        sb.append("2. **Suggestion title**: specific actionable recommendation\n");
        sb.append("(output as many items as needed)");
        return sb.toString();
    }
}
