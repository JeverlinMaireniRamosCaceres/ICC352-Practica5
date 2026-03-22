package servicios;

import entidades.Articulo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class ArticuloServices extends GestionDb<Articulo>{

    private static ArticuloServices instancia;

    private ArticuloServices() {
        super(Articulo.class);
    }

    public static ArticuloServices getInstancia() {
        if (instancia == null) {
            instancia = new ArticuloServices();
        }
        return instancia;
    }

    public List<Articulo> listarOrdenados() {
        EntityManager em = getEntityManager();
        Query query = em.createQuery(
                "select distinct a from Articulo a " +
                        "left join fetch a.listaEtiquetas " +
                        "order by a.fecha desc"
        );
        List<Articulo> lista = query.getResultList();
        em.close();
        return lista;
    }

    public boolean existeArticulo(String titulo, String cuerpo, String username) {
        EntityManager em = getEntityManager();
        try {
            var q = em.createQuery("""
            select count(a)
            from Articulo a
            where a.titulo = :titulo
              and a.cuerpo = :cuerpo
              and a.autor.username = :username
        """, Long.class);

            q.setParameter("titulo", titulo.trim());
            q.setParameter("cuerpo", cuerpo.trim());
            q.setParameter("username", username.trim());

            return q.getSingleResult() > 0;
        } finally {
            em.close();
        }
    }

    public Articulo findIdConDet(long id) {
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery(
                    "select distinct a from Articulo a " +
                            "left join fetch a.listaEtiquetas " +
                            "left join fetch a.listaComentarios c " +
                            "left join fetch c.autor " +
                            "where a.id = :id"
            );
            q.setParameter("id", id);

            List<Articulo> lista = q.getResultList();
            return lista.isEmpty() ? null : lista.get(0);
        } finally {
            em.close();
        }
    }

    public List<Articulo> listarPorEtiqueta(String etiqueta) {
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery(
                    "select distinct a from Articulo a " +
                            "join a.listaEtiquetas e " +
                            "left join fetch a.listaEtiquetas " +
                            "where lower(e.etiqueta) = lower(:etiqueta) " +
                            "order by a.fecha desc"
            );
            q.setParameter("etiqueta", etiqueta);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public List<Articulo> listarPaginado(int page, int size) {

        int offset = (page - 1) * size;

        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery(
                    "select distinct a from Articulo a " +
                            "left join fetch a.listaEtiquetas " +
                            "order by a.fecha desc"
            );

            q.setFirstResult(offset);
            q.setMaxResults(size);

            return q.getResultList();

        } finally {
            em.close();
        }
    }

    public long contarArticulos() {
        EntityManager em = getEntityManager();
        try {
            Query q = em.createQuery("select count(a) from Articulo a");
            return (long) q.getSingleResult();
        } finally {
            em.close();
        }
    }

}
