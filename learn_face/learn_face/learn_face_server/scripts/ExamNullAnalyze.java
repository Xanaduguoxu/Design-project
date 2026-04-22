import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExamNullAnalyze {
    private static final Pattern P_MANUAL_TRUE = Pattern.compile("\\\"manualRequired\\\"\\s*:\\s*true");
    private static final Pattern P_MANUAL_FALSE = Pattern.compile("\\\"manualRequired\\\"\\s*:\\s*false");
    private static final Pattern P_MANUAL_GRADED_FALSE = Pattern.compile("\\\"manualGraded\\\"\\s*:\\s*false");
    private static final Pattern P_PENDING = Pattern.compile("待批改");

    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://124.222.13.127:3306/learn_face?serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8";
        String user = "learn_face";
        String pwd = "123456";

        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(url, user, pwd);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, create_by, name, fin_score, answer FROM exam WHERE fin_score IS NULL ORDER BY id DESC")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                String createBy = rs.getString("create_by");
                String name = rs.getString("name");
                String answer = rs.getString("answer");
                int mTrue = count(P_MANUAL_TRUE, answer);
                int mFalse = count(P_MANUAL_FALSE, answer);
                int gFalse = count(P_MANUAL_GRADED_FALSE, answer);
                int pending = count(P_PENDING, answer);
                System.out.println("id=" + id + ", createBy=" + createBy + ", name=" + name + ", manualTrue=" + mTrue + ", manualFalse=" + mFalse + ", manualGradedFalse=" + gFalse + ", pendingText=" + pending);
            }
        }
    }

    private static int count(Pattern p, String text) {
        if (text == null) return 0;
        int c = 0;
        Matcher m = p.matcher(text);
        while (m.find()) c++;
        return c;
    }
}
