import java.sql.*;

public class ExamNullCheck {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://124.222.13.127:3306/learn_face?serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8";
        String user = "learn_face";
        String pwd = "123456";

        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(url, user, pwd);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, create_by, name, total_score, fin_score, JSON_LENGTH(answer) AS qn, answer FROM exam WHERE fin_score IS NULL ORDER BY id DESC LIMIT 10")) {
            while (rs.next()) {
                System.out.println("\nID=" + rs.getLong("id") + ", create_by=" + rs.getString("create_by") + ", name=" + rs.getString("name") + ", total=" + rs.getInt("total_score") + ", qn=" + rs.getInt("qn"));
                String ans = rs.getString("answer");
                if (ans != null && ans.length() > 1200) {
                    ans = ans.substring(0, 1200) + "...";
                }
                System.out.println(ans);
            }
        }
    }
}
