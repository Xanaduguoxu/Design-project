import java.sql.*;
import java.util.*;

public class QuestionSeeder {

    private static final String DB_URL = "jdbc:mysql://124.222.13.127:3306/learn_face?serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8";
    private static final String DB_USER = "learn_face";
    private static final String DB_PASSWORD = "123456";

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        List<QuestionItem> data = buildQuestions();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            conn.setAutoCommit(false);

            Set<String> columns = getColumns(conn, "question");
            List<String> insertColumns = buildInsertColumns(columns);
            String insertSql = buildInsertSql(insertColumns);

            int inserted = 0;
            int skipped = 0;

            try (PreparedStatement existsStmt = conn.prepareStatement("SELECT COUNT(1) FROM question WHERE question = ? AND category = ?");
                 PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

                for (QuestionItem item : data) {
                    existsStmt.setString(1, item.question);
                    existsStmt.setString(2, item.category);
                    try (ResultSet rs = existsStmt.executeQuery()) {
                        rs.next();
                        if (rs.getInt(1) > 0) {
                            skipped++;
                            continue;
                        }
                    }

                    bindInsert(insertStmt, insertColumns, item);
                    insertStmt.executeUpdate();
                    inserted++;
                }
            }

            conn.commit();
            System.out.println("题库导入完成。新增: " + inserted + "，跳过(已存在): " + skipped + "。");
        }
    }

    private static Set<String> getColumns(Connection conn, String tableName) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, tableName, "%")) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }
        return columns;
    }

    private static List<String> buildInsertColumns(Set<String> columns) {
        List<String> list = new ArrayList<>();
        list.add("question");
        list.add("choice");
        list.add("answer");
        list.add("parse");
        list.add("category");
        list.add("score");
        if (columns.contains("type")) list.add("type");
        if (columns.contains("knowledge_point")) list.add("knowledge_point");
        if (columns.contains("create_by")) list.add("create_by");
        if (columns.contains("update_by")) list.add("update_by");
        if (columns.contains("create_time")) list.add("create_time");
        if (columns.contains("update_time")) list.add("update_time");
        if (columns.contains("del")) list.add("del");
        return list;
    }

    private static String buildInsertSql(List<String> columns) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO question (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(columns.get(i));
        }
        sb.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("?");
        }
        sb.append(")");
        return sb.toString();
    }

    private static void bindInsert(PreparedStatement ps, List<String> columns, QuestionItem item) throws SQLException {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        for (int i = 0; i < columns.size(); i++) {
            String col = columns.get(i);
            int idx = i + 1;
            switch (col) {
                case "question":
                    ps.setString(idx, item.question);
                    break;
                case "choice":
                    ps.setString(idx, item.choiceJson);
                    break;
                case "answer":
                    ps.setString(idx, item.answer);
                    break;
                case "parse":
                    ps.setString(idx, item.parse);
                    break;
                case "category":
                    ps.setString(idx, item.category);
                    break;
                case "score":
                    ps.setInt(idx, item.score);
                    break;
                case "type":
                    ps.setString(idx, item.type);
                    break;
                case "knowledge_point":
                    ps.setString(idx, item.knowledgePoint);
                    break;
                case "create_by":
                case "update_by":
                    ps.setString(idx, "seed-script");
                    break;
                case "create_time":
                case "update_time":
                    ps.setTimestamp(idx, now);
                    break;
                case "del":
                    ps.setInt(idx, 0);
                    break;
                default:
                    ps.setObject(idx, null);
            }
        }
    }

    private static String choices(String... options) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < options.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(options[i].replace("\"", "\\\"")).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private static List<QuestionItem> buildQuestions() {
        List<QuestionItem> list = new ArrayList<>();

        list.add(QuestionItem.single(
                "在 Java 中，String 被设计为不可变对象，主要原因不包括以下哪一项？",
                choices("A. 便于缓存和复用字符串常量", "B. 让字符串拼接性能始终最优", "C. 提升线程安全性", "D. 提高哈希值稳定性"),
                "B", "String 不可变有利于常量池复用、线程安全和哈希稳定，但并不意味着字符串拼接始终最优。", 3, "简单", "Java基础"));
        list.add(QuestionItem.single(
                "HTTP 状态码 304 表示：",
                choices("A. 资源不存在", "B. 服务端错误", "C. 资源未修改，可使用缓存", "D. 请求参数错误"),
                "C", "304 Not Modified 表示客户端缓存仍有效。", 3, "简单", "HTTP缓存"));
        list.add(QuestionItem.single(
                "在 MySQL InnoDB 中，用于保证事务持久性的核心机制是：",
                choices("A. Redo Log", "B. Binlog", "C. 慢查询日志", "D. 查询缓存"),
                "A", "Redo Log 保证崩溃恢复后已提交事务的数据可重做。", 3, "中等", "MySQL事务"));
        list.add(QuestionItem.single(
                "Vue 中 v-model 在输入框上的本质通常是：",
                choices("A. :class + @click", "B. :value + @input", "C. v-if + v-show", "D. ref + watchEffect"),
                "B", "在 input 上本质是绑定 value 并监听 input 事件。", 3, "简单", "Vue基础"));
        list.add(QuestionItem.single(
                "TCP 三次握手的主要目标是：",
                choices("A. 提升带宽", "B. 双方确认收发能力并建立连接", "C. 减少延迟", "D. 提高应用层吞吐"),
                "B", "三次握手用于建立可靠连接并同步初始序列号。", 3, "简单", "计算机网络"));
        list.add(QuestionItem.single(
                "Spring Boot 项目的启动类通常使用的核心注解是：",
                choices("A. @EnableWebMvc", "B. @ConfigurationProperties", "C. @SpringBootApplication", "D. @MapperScan"),
                "C", "@SpringBootApplication 是组合注解，负责自动配置等。", 3, "简单", "SpringBoot"));
        list.add(QuestionItem.single(
                "在 Redis 中，为键设置过期时间的常用命令是：",
                choices("A. EXPIRE", "B. APPEND", "C. HSET", "D. KEYS"),
                "A", "EXPIRE key seconds 用于设置过期时间。", 3, "简单", "Redis"));
        list.add(QuestionItem.single(
                "二分查找在有序数组上的时间复杂度是：",
                choices("A. O(1)", "B. O(log n)", "C. O(n)", "D. O(n log n)"),
                "B", "每次将搜索区间缩小一半，复杂度 O(log n)。", 3, "简单", "算法基础"));

        list.add(QuestionItem.multi(
                "以下哪些属于关系型数据库？",
                choices("A. MySQL", "B. PostgreSQL", "C. SQLite", "D. Redis"),
                "ABC", "MySQL/PostgreSQL/SQLite 均为关系型数据库，Redis 为键值数据库。", 4, "简单", "数据库基础"));
        list.add(QuestionItem.multi(
                "以下 HTTP 方法通常被认为是幂等的有：",
                choices("A. GET", "B. PUT", "C. DELETE", "D. HEAD"),
                "ABCD", "GET/PUT/DELETE/HEAD 通常视为幂等（语义层面）。", 4, "中等", "HTTP方法"));
        list.add(QuestionItem.multi(
                "以下哪些 Java 容器/实现是线程安全的？",
                choices("A. Vector", "B. Hashtable", "C. ConcurrentHashMap", "D. ArrayList"),
                "ABC", "Vector/Hashtable/ConcurrentHashMap 是线程安全实现，ArrayList 不是。", 4, "中等", "Java集合"));
        list.add(QuestionItem.multi(
                "关于 MySQL 索引，以下说法正确的是：",
                choices("A. 可提升查询效率", "B. 过多索引会增加写入成本", "C. 联合索引受最左前缀影响", "D. 索引会占用额外存储"),
                "ABCD", "以上均正确。", 4, "中等", "MySQL索引"));
        list.add(QuestionItem.multi(
                "关于 Vue 生命周期，以下说法正确的是：",
                choices("A. created 阶段可访问 data", "B. mounted 阶段可访问真实 DOM", "C. beforeDestroy 适合清理定时器", "D. watch 可监听响应式数据变化"),
                "ABCD", "以上均为常见正确实践。", 4, "中等", "Vue生命周期"));
        list.add(QuestionItem.multi(
                "以下哪些属于 TCP 与 UDP 的典型差异？",
                choices("A. TCP 面向连接，UDP 无连接", "B. TCP 提供可靠传输，UDP 不保证可靠", "C. UDP 头部通常更小", "D. TCP 更适合强实时且极低延迟场景"),
                "ABC", "D 不是绝对结论，强实时场景常用 UDP。", 4, "中等", "传输层协议"));
        list.add(QuestionItem.multi(
                "以下哪些是 Spring 中常见的组件注解？",
                choices("A. @Component", "B. @Service", "C. @Repository", "D. @Controller"),
                "ABCD", "这些注解都用于标识组件并交给容器管理。", 4, "简单", "Spring基础"));
        list.add(QuestionItem.multi(
                "在 Git 协作中，以下哪些做法通常是推荐的？",
                choices("A. 小步提交，信息清晰", "B. 提交前先同步主干", "C. 直接在主分支长期开发", "D. 合并前进行代码评审"),
                "ABD", "直接在主分支长期开发通常不推荐。", 4, "中等", "Git协作"));

        list.add(QuestionItem.judge(
                "HTTPS 的默认端口是 443。", "正确", "HTTPS 默认端口为 443。", 2, "简单", "HTTPS"));
        list.add(QuestionItem.judge(
                "在 Java 中，使用 == 比较两个字符串对象一定是比较内容是否相同。", "错误", "== 比较引用地址，内容比较应使用 equals。", 2, "简单", "Java基础"));
        list.add(QuestionItem.judge(
                "MySQL 事务隔离级别越高，通常并发性能越容易下降。", "正确", "隔离级别提升通常带来更多锁/一致性开销。", 2, "中等", "MySQL事务"));
        list.add(QuestionItem.judge(
                "Vue 的计算属性（computed）推荐执行异步请求并修改多个外部状态。", "错误", "computed 应保持纯计算特性，不建议副作用。", 2, "中等", "Vue计算属性"));
        list.add(QuestionItem.judge(
                "Redis 可以同时开启 RDB 和 AOF 持久化。", "正确", "Redis 支持同时开启两种持久化策略。", 2, "中等", "Redis持久化"));
        list.add(QuestionItem.judge(
                "二叉搜索树进行中序遍历，结果是有序序列。", "正确", "BST 中序遍历得到升序序列。", 2, "简单", "数据结构"));
        list.add(QuestionItem.judge(
                "HTTP 500 状态码通常表示客户端参数格式错误。", "错误", "500 是服务端内部错误，客户端参数错误常见 400。", 2, "简单", "HTTP状态码"));

        list.add(QuestionItem.essay(
                "请说明 MySQL 索引失效的常见场景，并给出对应的优化建议。",
                "参考要点：函数操作列、隐式类型转换、前导模糊匹配、联合索引未遵守最左前缀、统计信息失真。优化可从 SQL 改写、索引重建、执行计划分析、分库分表等角度展开。",
                "应从失效原因、定位手段（EXPLAIN）、优化措施三部分作答，体现可执行性。", 10, "困难", "MySQL索引优化"));
        list.add(QuestionItem.essay(
                "请解释 JWT 的基本结构与认证流程，并说明其安全风险及防护措施。",
                "参考要点：Header.Payload.Signature；登录签发、请求携带、服务端验签；风险包含泄漏、过期策略不当、弱签名算法，防护包括 HTTPS、短有效期、黑名单与刷新机制。",
                "答题需体现结构、流程、风险、防护四个层面。", 10, "困难", "JWT认证"));
        list.add(QuestionItem.essay(
                "请给出一个高并发秒杀系统的设计思路，重点说明如何防止超卖。",
                "参考要点：限流、库存预扣、消息队列削峰、幂等、分布式锁或原子扣减、最终一致性补偿。",
                "重点考查并发控制与一致性设计，不要求唯一答案。", 10, "困难", "高并发架构"));
        list.add(QuestionItem.essay(
                "请比较进程与线程的差异，并说明在服务端开发中如何选择。",
                "参考要点：资源隔离、调度开销、通信方式、崩溃影响范围；结合 CPU 密集和 IO 密集场景做选择。",
                "应体现概念对比 + 场景决策逻辑。", 10, "中等", "操作系统"));
        list.add(QuestionItem.essay(
                "请描述 JVM 运行时内存区域，并举例说明常见 OOM 类型及排查思路。",
                "参考要点：堆、方法区/元空间、虚拟机栈、本地方法栈、程序计数器；常见 OOM 如 Java heap space、Metaspace、GC overhead。",
                "应包含区域职责、异常类型、排查工具（jmap/jstat/GC 日志）等。", 10, "困难", "JVM"));
        list.add(QuestionItem.essay(
                "请说明 Vue2 与 Vue3 响应式实现的主要差异，以及对开发体验的影响。",
                "参考要点：Vue2 基于 Object.defineProperty，Vue3 基于 Proxy；深层监听、数组处理、性能与类型推导支持差异。",
                "答题需有原理、差异、工程影响三个层次。", 10, "困难", "Vue响应式"));
        list.add(QuestionItem.essay(
                "请给出一套 SQL 慢查询优化流程，从发现问题到验证效果。",
                "参考要点：开启慢日志、定位 SQL、EXPLAIN 分析、索引与语句改写、回归压测与监控。",
                "应体现流程闭环与可量化验证。", 10, "中等", "SQL优化"));

        return list;
    }

    private static class QuestionItem {
        String question;
        String choiceJson;
        String answer;
        String parse;
        String category;
        int score;
        String type;
        String knowledgePoint;

        static QuestionItem single(String question, String choiceJson, String answer, String parse, int score, String type, String knowledgePoint) {
            return item(question, choiceJson, answer, parse, "单选题", score, type, knowledgePoint);
        }

        static QuestionItem multi(String question, String choiceJson, String answer, String parse, int score, String type, String knowledgePoint) {
            return item(question, choiceJson, answer, parse, "多选题", score, type, knowledgePoint);
        }

        static QuestionItem judge(String question, String answer, String parse, int score, String type, String knowledgePoint) {
            return item(question, null, answer, parse, "判断题", score, type, knowledgePoint);
        }

        static QuestionItem essay(String question, String answer, String parse, int score, String type, String knowledgePoint) {
            return item(question, null, answer, parse, "简答题", score, type, knowledgePoint);
        }

        static QuestionItem item(String question, String choiceJson, String answer, String parse, String category, int score, String type, String knowledgePoint) {
            QuestionItem q = new QuestionItem();
            q.question = question;
            q.choiceJson = choiceJson;
            q.answer = answer;
            q.parse = parse;
            q.category = category;
            q.score = score;
            q.type = type;
            q.knowledgePoint = knowledgePoint;
            return q;
        }
    }
}
