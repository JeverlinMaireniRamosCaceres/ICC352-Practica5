package entidades;

import jakarta.persistence.*;
import kotlin.experimental.ExperimentalObjCName;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Articulo implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;

    @Lob
    private String cuerpo;

    @ManyToOne
    private Usuario autor;

    private LocalDateTime fecha;

    @OneToMany(mappedBy = "articulo", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Comentario> listaComentarios = new HashSet<>();

    @ManyToMany
    private Set<Etiqueta> listaEtiquetas = new HashSet<>();

    public Articulo(){

    }

    public Articulo(String titulo,String cuerpo,Usuario autor, LocalDateTime fecha){
        this.titulo = titulo;
        this.cuerpo = cuerpo;
        this.autor = autor;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public void setCuerpo(String cuerpo) {
        this.cuerpo = cuerpo;
    }

    public Usuario getAutor() {
        return autor;
    }

    public void setAutor(Usuario autor) {
        this.autor = autor;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Set<Comentario> getListaComentarios() {
        return listaComentarios;
    }

    public void setListaComentarios(Set<Comentario> listaComentarios) {
        this.listaComentarios = listaComentarios;
    }

    public Set<Etiqueta> getListaEtiquetas() {
        return listaEtiquetas;
    }

    public void setListaEtiquetas(Set<Etiqueta> listaEtiquetas) {
        this.listaEtiquetas = listaEtiquetas;
    }
}
