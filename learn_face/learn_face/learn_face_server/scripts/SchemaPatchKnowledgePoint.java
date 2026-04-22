import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class SchemaPatchKnowledgePoint {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://124.222.13.127:3306/learn_face?serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8";
        String user = "learn_face";
        String pwd = "123456";

        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(url, user, pwd);
             Statement st = conn.createStatement()) {
            conn.setAutoCommit(false);

            addColumnIfMissing(st, "question", "knowledge_point", "ALTER TABLE question ADD COLUMN knowledge_point varchar(255) DEFAULT NULL COMMENT '知识点标签'");
            addColumnIfMissing(st, "task", "knowledge_point", "ALTER TABLE task ADD COLUMN knowledge_point varchar(255) DEFAULT NULL COMMENT '知识点标签'");

            conn.commit();
            System.out.println("Schema patch done.");
        }
    }

    private static void addColumnIfMissing(Statement st, String table, String column, String alterSql) throws Exception {
        String checkSql = "SELECT COUNT(1) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='" + table + "' AND COLUMN_NAME='" + column + "'";
        try (ResultSet rs = st.executeQuery(checkSql)) {
            rs.next();
            int exists = rs.getInt(1);
            if (exists > 0) {
                System.out.println(table + "." + column + " already exists.");
                return;
            }
        }
        st.execute(alterSql);
        System.out.println("Added column: " + table + "." + column);
    }
}
