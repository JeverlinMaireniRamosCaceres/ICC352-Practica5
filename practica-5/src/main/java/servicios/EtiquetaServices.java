package servicios;

import entidades.Etiqueta;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.List;

public class EtiquetaServices extends GestionDb<Etiqueta> {

    private static EtiquetaServices instancia;

    private EtiquetaServices() {
        super(Etiqueta.class);
    }

    public static EtiquetaServices getInstancia() {
        if (instancia == null) instancia = new EtiquetaServices();
        return instancia;
    }

    public Etiqueta findByEtiqueta(String nombre) {
        EntityManager em = getEntityManager();
        Query q = em.createQuery("select e from Etiqueta e where lower(e.etiqueta) = lower(:nombre)");
        q.setParameter("nombre", nombre);
        List<Etiqueta> lista = q.getResultList();
        em.close();
        return lista.isEmpty() ? null : lista.get(0);
    }

    public List<Etiqueta> listarPrimeras(int limite) {

        EntityManager em = getEntityManager();
        try {
            return em.createQuery("""
                select e
                from Etiqueta e
                order by e.id desc
                """, Etiqueta.class)
                    .setMaxResults(limite)
                    .getResultList();
        } finally {
            em.close();
        }
    }


}
