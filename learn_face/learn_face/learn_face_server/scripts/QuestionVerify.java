import java.sql.*;

public class QuestionVerify {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://124.222.13.127:3306/learn_face?serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8";
        String user = "learn_face";
        String pwd = "123456";

        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(url, user, pwd)) {
            printScalar(conn, "SELECT DATABASE()");
            printScalar(conn, "SELECT COUNT(1) FROM question");
            printScalar(conn, "SELECT COUNT(1) FROM question WHERE create_by='seed-script'");
            printScalar(conn, "SELECT COUNT(1) FROM question WHERE question='在 Java 中，String 被设计为不可变对象，主要原因不包括以下哪一项？' AND category='单选题'");
            printScalar(conn, "SELECT COUNT(1) FROM task");

            System.out.println("最近5条题目：");
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, question, category, score, type, knowledge_point, del, create_by, create_time FROM question ORDER BY id DESC LIMIT 5")) {
                while (rs.next()) {
                    System.out.println(
                            rs.getLong("id") + " | " +
                            rs.getString("category") + " | " +
                            rs.getInt("score") + " | " +
                            n(rs.getString("type")) + " | " +
                            n(rs.getString("knowledge_point")) + " | del=" + rs.getInt("del") + " | " +
                            n(rs.getString("create_by")) + " | " +
                            rs.getTimestamp("create_time") + " | " +
                            n(rs.getString("question"))
                    );
                }
            }
        }
    }

    private static void printScalar(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            System.out.println(sql + " => " + rs.getString(1));
        }
    }

    private static String n(String s) {
        return s == null ? "NULL" : s;
    }
}
