package servicios;

import org.h2.tools.Server;

import java.sql.SQLException;

public class BootStrapServices {

    private static BootStrapServices instancia;

    private BootStrapServices() {
    }

    public static BootStrapServices getInstancia() {
        if (instancia == null) {
            instancia = new BootStrapServices();
        }
        return instancia;
    }

    public void startDb() {
        try {
            // Servidor TCP de H2
            Server.createTcpServer(
                    "-tcpPort", "9092",
                    "-tcpAllowOthers",
                    "-tcpDaemon",
                    "-ifNotExists"
            ).start();

            // Consola web de H2 en puerto fijo y accesible desde fuera del contenedor
            String status = Server.createWebServer(
                    "-web",
                    "-webAllowOthers",
                    "-webPort", "8082"
            ).start().getStatus();

            System.out.println("Status Web: " + status);

        } catch (SQLException ex) {
            System.out.println("Problema con la base de datos: " + ex.getMessage());
        }
    }

    public void init() {
        startDb();
    }
}