package servicios;

import entidades.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.List;

public class UsuarioServices extends GestionDb<Usuario>{

    private static UsuarioServices instancia;

    private UsuarioServices(){
        super(Usuario.class);
    }

    public static UsuarioServices getInstancia() {
        if (instancia == null) {
            instancia = new UsuarioServices();
        }
        return instancia;
    }

    public Usuario findByUsername(String username) {

        EntityManager em = getEntityManager();

        Query query = em.createQuery(
                "select u from Usuario u where u.username = :username"
        );

        query.setParameter("username", username);

        List<Usuario> lista = query.getResultList();

        em.close();

        if (lista.isEmpty()) {
            return null;
        }

        return lista.get(0);
    }


}
