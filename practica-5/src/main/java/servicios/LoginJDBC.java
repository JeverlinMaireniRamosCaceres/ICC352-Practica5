package servicios;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class LoginJDBC {

    private static final Logger log = LoggerFactory.getLogger(LoginJDBC.class);

    public static void registrarLogin(String username) {

        String url = System.getenv("JDBC_DATABASE_URL");

        if (url == null || url.isBlank()) {
            System.out.println("JDBC_DATABASE_URL no definida.");
            return;
        }

        String sql = "INSERT INTO login_audit (username, fecha) VALUES (?, now())";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.executeUpdate();

            System.out.println("Login registrado en CockroachDB.");

        } catch (Exception e) {
            e.printStackTrace();
            log.info("SQLState={}", e.getMessage());

        }
    }

}
