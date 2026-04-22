import java.sql.*;

public class ExamVerify {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://124.222.13.127:3306/learn_face?serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8";
        String user = "learn_face";
        String pwd = "123456";

        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(url, user, pwd)) {
            query(conn, "SELECT COUNT(1) AS c FROM exam");
            query(conn, "SELECT create_by, COUNT(1) AS c FROM exam GROUP BY create_by ORDER BY c DESC LIMIT 20");
            query(conn, "SELECT id, name, create_by, total_score, fin_score, create_time FROM exam ORDER BY id DESC LIMIT 20");
        }
    }

    private static void query(Connection conn, String sql) throws SQLException {
        System.out.println("\n-- " + sql);
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            while (rs.next()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= cols; i++) {
                    if (i > 1) sb.append(" | ");
                    sb.append(md.getColumnLabel(i)).append("=").append(rs.getString(i));
                }
                System.out.println(sb);
            }
        }
    }
}
