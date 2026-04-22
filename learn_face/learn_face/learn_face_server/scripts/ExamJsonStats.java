import java.sql.*;

public class ExamJsonStats {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://124.222.13.127:3306/learn_face?serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8";
        String user = "learn_face";
        String pwd = "123456";

        String sql = "SELECT e.id, e.create_by, e.name, e.fin_score, " +
                "COUNT(*) AS total_q, " +
                "SUM(CASE WHEN jt.manualRequired = TRUE THEN 1 ELSE 0 END) AS manual_q, " +
                "SUM(CASE WHEN jt.manualRequired = TRUE AND (jt.manualGraded = TRUE OR (jt.result IS NOT NULL AND jt.result <> '待批改')) THEN 1 ELSE 0 END) AS manual_done " +
                "FROM exam e " +
                "JOIN JSON_TABLE(e.answer, '$[*]' COLUMNS (" +
                "manualRequired BOOLEAN PATH '$.manualRequired' DEFAULT FALSE ON EMPTY, " +
                "manualGraded BOOLEAN PATH '$.manualGraded' DEFAULT NULL ON EMPTY, " +
                "result VARCHAR(32) PATH '$.result' DEFAULT NULL ON EMPTY" +
                ")) jt " +
                "WHERE e.fin_score IS NULL " +
                "GROUP BY e.id, e.create_by, e.name, e.fin_score " +
                "ORDER BY e.id DESC";

        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(url, user, pwd);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                System.out.println("id=" + rs.getLong("id") +
                        ", create_by=" + rs.getString("create_by") +
                        ", name=" + rs.getString("name") +
                        ", total_q=" + rs.getInt("total_q") +
                        ", manual_q=" + rs.getInt("manual_q") +
                        ", manual_done=" + rs.getInt("manual_done"));
            }
        }
    }
}
