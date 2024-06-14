package controller;

import controller.exceptions.NonexistentEntityException;
import controller.exceptions.PreexistingEntityException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import model.Pessoa;
import model.PessoaJuridica;

import java.util.List;

public class PessoaJuridicaJPAController {

    private final EntityManagerFactory emf;

    public PessoaJuridicaJPAController(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(PessoaJuridica pessoaJuridica) throws PreexistingEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Pessoa pessoaidPessoa = attachPessoaIfNotNull(em, pessoaJuridica.getPessoaidPessoa());
            pessoaJuridica.setPessoaidPessoa(pessoaidPessoa);
            em.persist(pessoaJuridica);
            mergePessoaJuridicaToPessoa(em, pessoaJuridica, pessoaidPessoa);
            em.getTransaction().commit();
        } catch (Exception ex) {
            handleCreateException(ex, pessoaJuridica.getCnpj());
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    private Pessoa attachPessoaIfNotNull(EntityManager em, Pessoa pessoa) {
        if (pessoa != null) {
            return em.getReference(pessoa.getClass(), pessoa.getIdPessoa());
        }
        return null;
    }

    private void mergePessoaJuridicaToPessoa(EntityManager em, PessoaJuridica pessoaJuridica, Pessoa pessoaidPessoa) {
        if (pessoaidPessoa != null) {
            pessoaidPessoa.getPessoaJuridicaCollection().add(pessoaJuridica);
            em.merge(pessoaidPessoa);
        }
    }

    private void handleCreateException(Exception ex, String id) throws PreexistingEntityException {
        if (findPessoaJuridica(id) != null) {
            throw new PreexistingEntityException("PessoaJuridica with id " + id + " already exists.", ex);
        }
        throw new PreexistingEntityException("Error creating PessoaJuridica.", ex);
    }

    public void edit(PessoaJuridica pessoaJuridica) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            PessoaJuridica persistentPessoaJuridica = em.find(PessoaJuridica.class, pessoaJuridica.getCnpj());
            Pessoa pessoaidPessoaOld = persistentPessoaJuridica.getPessoaidPessoa();
            Pessoa pessoaidPessoaNew = attachPessoaIfNotNull(em, pessoaJuridica.getPessoaidPessoa());
            pessoaJuridica.setPessoaidPessoa(pessoaidPessoaNew);
            pessoaJuridica = em.merge(pessoaJuridica);
            mergePessoaJuridicaToPessoa(em, pessoaJuridica, pessoaidPessoaOld, pessoaidPessoaNew);
            em.getTransaction().commit();
        } catch (Exception ex) {
            handleEditException(ex, pessoaJuridica.getCnpj());
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    private void mergePessoaJuridicaToPessoa(EntityManager em, PessoaJuridica pessoaJuridica, Pessoa pessoaidPessoaOld, Pessoa pessoaidPessoaNew) {
        if (pessoaidPessoaOld != null && !pessoaidPessoaOld.equals(pessoaidPessoaNew)) {
            pessoaidPessoaOld.getPessoaJuridicaCollection().remove(pessoaJuridica);
            em.merge(pessoaidPessoaOld);
        }
        if (pessoaidPessoaNew != null && !pessoaidPessoaNew.equals(pessoaidPessoaOld)) {
            pessoaidPessoaNew.getPessoaJuridicaCollection().add(pessoaJuridica);
            em.merge(pessoaidPessoaNew);
        }
    }

    private void handleEditException(Exception ex, String id) throws NonexistentEntityException {
        String msg = ex.getLocalizedMessage();
        if (msg == null || msg.length() == 0) {
            if (findPessoaJuridica(id) == null) {
                throw new NonexistentEntityException("The pessoaJuridica with id " + id + " no longer exists.");
            }
        }
        throw new NonexistentEntityException("Error editing PessoaJuridica.", ex);
    }

    public void destroy(String id) throws NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            PessoaJuridica pessoaJuridica = getReference(em, PessoaJuridica.class, id);
            Pessoa pessoaidPessoa = pessoaJuridica.getPessoaidPessoa();
            removePessoaJuridicaFromPessoa(em, pessoaJuridica, pessoaidPessoa);
            em.remove(pessoaJuridica);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    private <T> T getReference(EntityManager em, Class<T> entityClass, String id) {
        return em.getReference(entityClass, id);
    }

    private void removePessoaJuridicaFromPessoa(EntityManager em, PessoaJuridica pessoaJuridica, Pessoa pessoaidPessoa) {
        if (pessoaidPessoa != null) {
            pessoaidPessoa.getPessoaJuridicaCollection().remove(pessoaJuridica);
            em.merge(pessoaidPessoa);
        }
    }

    public List<PessoaJuridica> findPessoaJuridicaEntities() {
        return findPessoaJuridicaEntities(true, -1, -1);
    }

    public List<PessoaJuridica> findPessoaJuridicaEntities(int maxResults, int firstResult) {
        return findPessoaJuridicaEntities(false, maxResults, firstResult);
    }

    private List<PessoaJuridica> findPessoaJuridicaEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(PessoaJuridica.class));
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

    public PessoaJuridica findPessoaJuridica(String id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(PessoaJuridica.class, id);
        } finally {
            em.close();
        }
    }

    public int getPessoaJuridicaCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<PessoaJuridica> rt = cq.from(PessoaJuridica.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
}
