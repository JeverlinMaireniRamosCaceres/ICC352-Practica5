package entidades;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
public class Usuario implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    private String nombre;
    private String contrasena;
    private boolean administrador;
    private boolean autor;
    private boolean usuarioNormal;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String fotoBase64;

    private String fotoMimeType;


    public Usuario(){

    }

    public Usuario(String username, String nombre, String contrasena, boolean administrador, boolean autor,boolean usuarioNormal){
        this.username = username;
        this.nombre = nombre;
        this.contrasena = contrasena;
        this.administrador = administrador;
        this.autor = autor;
        this.usuarioNormal = usuarioNormal;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public boolean isAdministrador() {
        return administrador;
    }

    public void setAdministrador(boolean administrador) {
        this.administrador = administrador;
    }

    public boolean isAutor() {
        return autor;
    }

    public void setAutor(boolean autor) {
        this.autor = autor;
    }

    public boolean isUsuarioNormal() {
        return usuarioNormal;
    }

    public void setUsuarioNormal(boolean usuarioNormal) {
        this.usuarioNormal = usuarioNormal;
    }

    public String getFotoBase64() {
        return fotoBase64;
    }

    public void setFotoBase64(String fotoBase64) {
        this.fotoBase64 = fotoBase64;
    }

    public String getFotoMimeType() {
        return fotoMimeType;
    }

    public void setFotoMimeType(String fotoMimeType) {
        this.fotoMimeType = fotoMimeType;
    }
}
