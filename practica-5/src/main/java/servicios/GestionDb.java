package servicios;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaQuery;
import java.util.List;

public class GestionDb<T> {

    private static EntityManagerFactory emf;
    private final Class<T> claseEntidad;

    public GestionDb(Class<T> claseEntidad) {
        if (emf == null) {
            emf = Persistence.createEntityManagerFactory("MiUnidadPersistencia");
        }
        this.claseEntidad = claseEntidad;
    }

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public T crear(T entidad) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(entidad);
            em.getTransaction().commit();
            return entidad;
        } finally {
            em.close();
        }
    }

    public T editar(T entidad) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            T actualizado = em.merge(entidad);
            em.getTransaction().commit();
            return actualizado;
        } finally {
            em.close();
        }
    }

    public boolean eliminar(Object id) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            T entidad = em.find(claseEntidad, id);
            if (entidad == null) return false;
            em.remove(entidad);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    public T find(Object id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(claseEntidad, id);
        } finally {
            em.close();
        }
    }

    public List<T> findAll() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery<T> cq = em.getCriteriaBuilder().createQuery(claseEntidad);
            cq.select(cq.from(claseEntidad));
            return em.createQuery(cq).getResultList();
        } finally {
            em.close();
        }
    }
}