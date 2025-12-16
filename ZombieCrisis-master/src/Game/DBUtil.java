package Game;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 数据库连接工具类，负责获取连接和关闭资源
 */
public class DBUtil {
    private static final String URL = "jdbc:mysql://localhost:3306/zombie_game?useSSL=false&serverTimezone=UTC&characterEncoding=utf8&allowPublicKeyRetrieval=true";
    private static final String USER = "root";       // 数据库用户名
    private static final String PASSWORD = "Cai123456"; // 数据库密码（替换为你的密码）

    static {
        try {
            // 加载MySQL驱动（MySQL 8.0+使用com.mysql.cj.jdbc.Driver）
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("数据库驱动加载失败，请检查依赖是否正确");
        }
    }

    /**
     * 获取数据库连接
     * @return 数据库连接对象
     * @throws SQLException 连接失败时抛出异常
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * 关闭数据库资源（自动关闭接口，支持Connection、PreparedStatement、ResultSet）
     * @param resources 可变参数，传入需要关闭的资源
     */
    public static void close(AutoCloseable... resources) {
        for (AutoCloseable res : resources) {
            if (res != null) {
                try {
                    res.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}