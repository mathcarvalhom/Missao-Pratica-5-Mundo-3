package controller;

import controller.exceptions.IllegalOrphanException;
import controller.exceptions.NonexistentEntityException;

import model.Movimento;
import model.Usuario;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UsuarioJPAController {

    private final EntityManagerFactory emf;

    public UsuarioJPAController(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Usuario usuario) {
        if (usuario.getMovimentoCollection() == null) {
            usuario.setMovimentoCollection(new ArrayList<>());
        }
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            usuario.setMovimentoCollection(attachMovimentoCollection(em, usuario.getMovimentoCollection()));
            em.persist(usuario);
            mergeMovimentoCollectionToUsuario(em, usuario);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    private Collection<Movimento> attachMovimentoCollection(EntityManager em, Collection<Movimento> movimentoCollection) {
        Collection<Movimento> attachedMovimentoCollection = new ArrayList<>();
        for (Movimento movimentoCollectionMovimentoToAttach : movimentoCollection) {
            attachedMovimentoCollection.add(em.getReference(Movimento.class, movimentoCollectionMovimentoToAttach.getIdMovimento()));
        }
        return attachedMovimentoCollection;
    }

    private void mergeMovimentoCollectionToUsuario(EntityManager em, Usuario usuario) {
        for (Movimento movimentoCollectionMovimento : usuario.getMovimentoCollection()) {
            movimentoCollectionMovimento.setUsuarioidUsuario(usuario);
            em.merge(movimentoCollectionMovimento);
        }
    }

    public void edit(Usuario usuario) throws IllegalOrphanException, NonexistentEntityException, Exception {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            Usuario persistentUsuario = getUsuario(em, usuario.getIdUsuario());
            checkMovimentoCollection(em, persistentUsuario, usuario.getMovimentoCollection());
            usuario = mergeUsuario(em, usuario);
            mergeMovimentoCollectionToUsuario(em, usuario);
            em.getTransaction().commit();
        } catch (Exception ex) {
            handleEditException(ex, usuario.getIdUsuario());
        } finally {
            em.close();
        }
    }

    private Usuario getUsuario(EntityManager em, Integer id) throws NonexistentEntityException {
        Usuario persistentUsuario = em.find(Usuario.class, id);
        if (persistentUsuario == null) {
            throw new NonexistentEntityException("The usuario with id " + id + " no longer exists.");
        }
        return persistentUsuario;
    }

    private void checkMovimentoCollection(EntityManager em, Usuario persistentUsuario, Collection<Movimento> movimentoCollection) throws IllegalOrphanException {
        Collection<Movimento> movimentoCollectionOld = persistentUsuario.getMovimentoCollection();
        List<String> illegalOrphanMessages = getIllegalOrphanMessages(movimentoCollectionOld, movimentoCollection);
        if (illegalOrphanMessages != null) {
            throw new IllegalOrphanException(illegalOrphanMessages);
        }
    }

    private Usuario mergeUsuario(EntityManager em, Usuario usuario) {
        return em.merge(usuario);
    }

    private List<String> getIllegalOrphanMessages(Collection<Movimento> movimentoCollectionOld, Collection<Movimento> movimentoCollectionNew) {
        List<String> illegalOrphanMessages = null;
        for (Movimento movimentoCollectionOldMovimento : movimentoCollectionOld) {
            if (!movimentoCollectionNew.contains(movimentoCollectionOldMovimento)) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<>();
                }
                illegalOrphanMessages.add("You must retain Movimento " + movimentoCollectionOldMovimento +
                        " since its usuarioidUsuario field is not nullable.");
            }
        }
        return illegalOrphanMessages;
    }

    private void handleEditException(Exception ex, Integer id) throws NonexistentEntityException {
        String msg = ex.getLocalizedMessage();
        if (msg == null || msg.length() == 0) {
            if (findUsuario(id) == null) {
                throw new NonexistentEntityException("The usuario with id " + id + " no longer exists.");
            }
        }
        throw new NonexistentEntityException("Error editing Usuario.", ex);
    }

    public void destroy(Integer id) throws IllegalOrphanException, NonexistentEntityException {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            Usuario usuario = getUsuario(em, id);
            checkMovimentoCollection(em, usuario, usuario.getMovimentoCollection());
            em.remove(usuario);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    public List<Usuario> findUsuarioEntities() {
        return findUsuarioEntities(true, -1, -1);
    }

    public List<Usuario> findUsuarioEntities(int maxResults, int firstResult) {
        return findUsuarioEntities(false, maxResults, firstResult);
    }

    private List<Usuario> findUsuarioEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Usuario.class));
            Query q = em.createQuery(cq);
            if (!all) {
                q.setMaxResults(maxResults);
                q.setFirstResult(firstResult);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public Usuario findUsuario(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Usuario.class, id);
        } finally {
            em.close();
        }
    }

    public Usuario findUsuario(String login, String senha) {
        EntityManager em = getEntityManager();
        try {
            List list = em.createQuery("SELECT u FROM Usuario u WHERE u.login = :login AND u.senha = :senha")
                    .setParameter("login", login)
                    .setParameter("senha", senha)
                    .getResultList();
            return list.isEmpty() ? null : (Usuario) list.get(0);
        } finally {
            em.close();
        }
    }

    public int getUsuarioCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Usuario> rt = cq.from(Usuario.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }

    private static class EntityManagerFactory {

        public EntityManagerFactory() {
        }

        private EntityManager createEntityManager() {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }
    }

    private static class IllegalOrphanException extends Exception {

        public IllegalOrphanException() {
        }

        private IllegalOrphanException(List<String> illegalOrphanMessages) {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }
    }

    private static class NonexistentEntityException extends Exception {

        public NonexistentEntityException() {
        }

        private NonexistentEntityException(String string) {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        private NonexistentEntityException(String error_editing_Usuario, Exception ex) {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }
    }
}
