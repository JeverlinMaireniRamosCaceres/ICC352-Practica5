package servicios;

import org.jasypt.util.text.BasicTextEncryptor;

public class ServiciosCripto {

    private static final BasicTextEncryptor encryptor = new BasicTextEncryptor();

    static {
        encryptor.setPassword("PRACTICA3");
    }

    public static String encriptar(String texto) {
        return encryptor.encrypt(texto);
    }

    public static String desencriptar(String texto) {
        return encryptor.decrypt(texto);
    }
}