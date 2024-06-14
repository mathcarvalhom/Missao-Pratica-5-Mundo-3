package controller;

import controller.exceptions.NonexistentEntityException;
import model.Movimento;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import java.util.List;

public class MovimentoJPAController {

    private final EntityManagerFactory emf;

    public MovimentoJPAController(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Movimento movimento) {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        try {
            em.persist(movimento);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    public void edit(Movimento movimento) throws NonexistentEntityException, Exception {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        try {
            movimento = em.merge(movimento);
            em.getTransaction().commit();
        } catch (Exception ex) {
            handleException(ex, movimento.getIdMovimento());
        } finally {
            em.close();
        }
    }

    public void destroy(Integer id) throws NonexistentEntityException {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        try {
            Movimento movimento = em.getReference(Movimento.class, id);
            em.remove(movimento);
            em.getTransaction().commit();
        } catch (EntityNotFoundException enfe) {
            handleException(enfe, id);
        } finally {
            em.close();
        }
    }

    public List<Movimento> findMovimentoEntities() {
        return findMovimentoEntities(true, -1, -1);
    }

    public List<Movimento> findMovimentoEntities(int maxResults, int firstResult) {
        return findMovimentoEntities(false, maxResults, firstResult);
    }

    private List<Movimento> findMovimentoEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Movimento.class));
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

    public Movimento findMovimento(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Movimento.class, id);
        } finally {
            em.close();
        }
    }

    public int getMovimentoCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Movimento> rt = cq.from(Movimento.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }

    private void handleException(Exception ex, Object id) throws NonexistentEntityException {
        String msg = ex.getLocalizedMessage();
        if (msg == null || msg.length() == 0) {
            throw new NonexistentEntityException("The movimento with id " + id + " no longer exists.");
        }
        throw new NonexistentEntityException(msg, ex);
    }
}
