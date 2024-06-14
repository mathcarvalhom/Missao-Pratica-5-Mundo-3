package controller;

import controller.exceptions.IllegalOrphanException;
import controller.exceptions.NonexistentEntityException;
import controller.exceptions.PreexistingEntityException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import model.Pessoa;
import model.PessoaFisica;
import model.PessoaJuridica;
import model.Movimento;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PessoaJPAController {

    private final EntityManagerFactory emf;

    public PessoaJPAController(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Pessoa pessoa) throws PreexistingEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            initializeCollectionsIfNull(pessoa);
            attachCollectionsToEntity(em, pessoa);
            em.persist(pessoa);
            mergeCollections(em, pessoa);
            em.getTransaction().commit();
        } catch (Exception ex) {
            handleCreateException(ex, pessoa.getIdPessoa());
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    private void initializeCollectionsIfNull(Pessoa pessoa) {
        if (pessoa.getPessoaJuridicaCollection() == null) {
            pessoa.setPessoaJuridicaCollection(new ArrayList<>());
        }
        if (pessoa.getPessoaFisicaCollection() == null) {
            pessoa.setPessoaFisicaCollection(new ArrayList<>());
        }
        if (pessoa.getMovimentoCollection() == null) {
            pessoa.setMovimentoCollection(new ArrayList<>());
        }
    }

    private void attachCollectionsToEntity(EntityManager em, Pessoa pessoa) {
        pessoa.setPessoaJuridicaCollection(attachCollectionToEntity(em, pessoa.getPessoaJuridicaCollection(), PessoaJuridica.class));
        pessoa.setPessoaFisicaCollection(attachCollectionToEntity(em, pessoa.getPessoaFisicaCollection(), PessoaFisica.class));
        pessoa.setMovimentoCollection(attachCollectionToEntity(em, pessoa.getMovimentoCollection(), Movimento.class));
    }

    private <T> Collection<T> attachCollectionToEntity(EntityManager em, Collection<T> collection, Class<T> entityClass) {
        Collection<T> attachedCollection = new ArrayList<>();
        for (T item : collection) {
            attachedCollection.add(em.getReference(entityClass, emf.getPersistenceUnitUtil().getIdentifier(item)));
        }
        return attachedCollection;
    }

    private void mergeCollections(EntityManager em, Pessoa pessoa) {
        mergeCollection(em, pessoa.getPessoaJuridicaCollection());
        mergeCollection(em, pessoa.getPessoaFisicaCollection());
        mergeCollection(em, pessoa.getMovimentoCollection());
    }

    private <T> void mergeCollection(EntityManager em, Collection<T> collection) {
        for (T item : collection) {
            em.merge(item);
        }
    }

    private void handleCreateException(Exception ex, Integer id) throws PreexistingEntityException {
        if (findPessoa(id) != null) {
            throw new PreexistingEntityException("Pessoa with id " + id + " already exists.", ex);
        }
        throw new PreexistingEntityException("Error creating Pessoa.", ex);
    }

    public void edit(Pessoa pessoa) throws IllegalOrphanException, NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Pessoa persistentPessoa = em.find(Pessoa.class, pessoa.getIdPessoa());
            checkForIllegalOrphans(pessoa, persistentPessoa);
            attachCollectionsToEntity(em, pessoa);
            pessoa = em.merge(pessoa);
            mergeCollections(em, pessoa);
            em.getTransaction().commit();
        } catch (Exception ex) {
            handleEditException(ex, pessoa.getIdPessoa());
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    private void checkForIllegalOrphans(Pessoa pessoa, Pessoa persistentPessoa) throws IllegalOrphanException {
        List<String> illegalOrphanMessages = new ArrayList<>();
        checkForIllegalOrphans(pessoa.getPessoaJuridicaCollection(), persistentPessoa.getPessoaJuridicaCollection(), "pessoaidPessoa", illegalOrphanMessages);
        checkForIllegalOrphans(pessoa.getPessoaFisicaCollection(), persistentPessoa.getPessoaFisicaCollection(), "pessoaidPessoa", illegalOrphanMessages);
        checkForIllegalOrphans(pessoa.getMovimentoCollection(), persistentPessoa.getMovimentoCollection(), "pessoaidPessoa", illegalOrphanMessages);

        if (!illegalOrphanMessages.isEmpty()) {
            throw new IllegalOrphanException(illegalOrphanMessages);
        }
    }

    private <T> void checkForIllegalOrphans(Collection<T> newCollection, Collection<T> oldCollection, String foreignKey, List<String> illegalOrphanMessages) {
        for (T oldItem : oldCollection) {
            if (!newCollection.contains(oldItem)) {
                illegalOrphanMessages.add("You must retain " + oldItem.getClass().getSimpleName() + " " + oldItem + " since its " + foreignKey + " field is not nullable.");
            }
        }
    }

    private void handleEditException(Exception ex, Integer id) throws NonexistentEntityException {
        String msg = ex.getLocalizedMessage();
        if (msg == null || msg.length() == 0) {
            if (findPessoa(id) == null) {
                throw new NonexistentEntityException("The pessoa with id " + id + " no longer exists.");
            }
        }
        throw new NonexistentEntityException("Error editing Pessoa.", ex);
    }

    public void destroy(Integer id) throws IllegalOrphanException, NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Pessoa pessoa = em.getReference(Pessoa.class, id);
            checkForIllegalOrphans(pessoa.getPessoaJuridicaCollection(), "pessoaidPessoa");
            checkForIllegalOrphans(pessoa.getPessoaFisicaCollection(), "pessoaidPessoa");
            checkForIllegalOrphans(pessoa.getMovimentoCollection(), "pessoaidPessoa");
            em.remove(pessoa);
            em.getTransaction().commit();
        } catch (Exception ex) {
            handleDestroyException(ex, id);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    private void checkForIllegalOrphans(Collection<?> collection, String foreignKey) throws IllegalOrphanException {
        List<String> illegalOrphanMessages = new ArrayList<>();
        for (Object item : collection) {
            illegalOrphanMessages.add("This Pessoa cannot be destroyed since the " + item.getClass().getSimpleName() + " " + item + " in its " + foreignKey + " field has a non-nullable " + foreignKey + " field.");
        }

        if (!illegalOrphanMessages.isEmpty()) {
            throw new IllegalOrphanException(illegalOrphanMessages);
        }
    }

    private void handleDestroyException(Exception ex, Integer id) throws NonexistentEntityException {
        String msg = ex.getLocalizedMessage();
        if (msg == null || msg.length() == 0) {
            if (findPessoa(id) == null) {
                throw new NonexistentEntityException("The pessoa with id " + id + " no longer exists.");
            }
        }
        throw new NonexistentEntityException("Error destroying Pessoa.", ex);
    }

    public List<Pessoa> findPessoaEntities() {
        return findPessoaEntities(true, -1, -1);
    }

    public List<Pessoa> findPessoaEntities(int maxResults, int firstResult) {
        return findPessoaEntities(false, maxResults, firstResult);
    }

    private List<Pessoa> findPessoaEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Pessoa.class));
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

    public Pessoa findPessoa(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Pessoa.class, id);
        } finally {
            em.close();
        }
    }

    public int getPessoaCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Pessoa> rt = cq.from(Pessoa.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }

    private void checkForIllegalOrphans(Object pessoaJuridicaCollection, String pessoaidPessoa) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
