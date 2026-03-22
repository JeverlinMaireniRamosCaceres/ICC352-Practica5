package servicios;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class LoginJDBC {

    private static final Logger log = LoggerFactory.getLogger(LoginJDBC.class);

    private static void crearTablaSiNoExiste(Connection conn) throws Exception {
        String sql = """
                CREATE TABLE IF NOT EXISTS login_audit (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    username STRING NOT NULL,
                    fecha TIMESTAMP NOT NULL DEFAULT now()
                )
                """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public static void inicializarTabla() {
        String url = System.getenv("JDBC_DATABASE_URL");

        if (url == null || url.isBlank()) {
            System.out.println("JDBC_DATABASE_URL no definida.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(url)) {
            crearTablaSiNoExiste(conn);
            System.out.println("Tabla login_audit verificada/creada.");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error creando tabla login_audit: {}", e.getMessage());
        }
    }

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
            log.error("Error registrando login: {}", e.getMessage());
        }
    }
}