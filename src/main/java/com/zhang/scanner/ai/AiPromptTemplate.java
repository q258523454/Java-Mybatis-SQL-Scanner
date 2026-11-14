package com.zhang.scanner.ai;

/**
 * Ask AI 提示词模板(固定结构, 语言跟随插件语言设置)
 */
public final class AiPromptTemplate {

    private AiPromptTemplate() {
    }

    /**
     * 系统提示词(角色定位)
     *
     * @param chinese 是否中文
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
                + "opportunities in the SQL, and give specific, actionable suggestions.";
    }

    /**
     * 用户提示词(固定模板, 包含四段上下文)
     *
     * @param sqlNodeId    SQL ID
     * @param sqlType      SQL Type(数据库类型)
     * @param sqlText      格式化后的 SQL
     * @param parseResult  报错信息或建议(空时显示"（无）")
     */
    public static String buildUserPrompt(String sqlNodeId, String sqlType, String sqlText, String parseResult) {
        String suggestion = (null == parseResult || parseResult.trim().isEmpty()) ? "（无）" : parseResult;
        StringBuilder sb = new StringBuilder();
        sb.append("请分析以下 MyBatis 映射文件中的 SQL：\n\n");
        sb.append("【SQL ID】\n").append(sqlNodeId).append("\n\n");
        sb.append("【SQL Type】\n").append(sqlType).append("\n\n");
        sb.append("【格式化后的 SQL】\n");
        sb.append("```sql\n").append(sqlText).append("\n```\n\n");
        sb.append("【报错信息或建议】\n").append(suggestion).append("\n\n");
        sb.append("请从正确性、性能、索引使用、潜在风险等角度分析这条 SQL 是否存在问题或需要优化的地方。");
        return sb.toString();
    }
}
