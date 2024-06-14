package controller;

import controller.exceptions.IllegalOrphanException;
import controller.exceptions.NonexistentEntityException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import model.Movimento;
import model.Produto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ProdutoJPAController {

    private final EntityManagerFactory emf;

    public ProdutoJPAController(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Produto produto) {
        if (produto.getMovimentoCollection() == null) {
            produto.setMovimentoCollection(new ArrayList<Movimento>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Collection<Movimento> attachedMovimentoCollection = attachMovimentoCollection(em, (Collection<Movimento>) produto.getMovimentoCollection());
            produto.setMovimentoCollection(attachedMovimentoCollection);
            em.persist(produto);
            mergeMovimentoCollectionToProduto(em, produto);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    private Collection<Movimento> attachMovimentoCollection(EntityManager em, Collection<Movimento> movimentoCollection) {
        Collection<Movimento> attachedMovimentoCollection = new ArrayList<>();
        for (Movimento movimentoCollectionMovimentoToAttach : movimentoCollection) {
            movimentoCollectionMovimentoToAttach = em.getReference(movimentoCollectionMovimentoToAttach.getClass(), movimentoCollectionMovimentoToAttach.getIdMovimento());
            attachedMovimentoCollection.add(movimentoCollectionMovimentoToAttach);
        }
        return attachedMovimentoCollection;
    }

    private void mergeMovimentoCollectionToProduto(EntityManager em, Produto produto) {
        for (Movimento movimentoCollectionMovimento : produto.getMovimentoCollection()) {
            Produto oldProdutoidProdutoOfMovimentoCollectionMovimento = movimentoCollectionMovimento.getProdutoidProduto();
            movimentoCollectionMovimento.setProdutoidProduto(produto);
            movimentoCollectionMovimento = em.merge(movimentoCollectionMovimento);
            if (oldProdutoidProdutoOfMovimentoCollectionMovimento != null && !oldProdutoidProdutoOfMovimentoCollectionMovimento.equals(produto)) {
                oldProdutoidProdutoOfMovimentoCollectionMovimento.getMovimentoCollection().remove(movimentoCollectionMovimento);
                em.merge(oldProdutoidProdutoOfMovimentoCollectionMovimento);
            }
        }
    }

    public void edit(Produto produto) throws IllegalOrphanException, NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Produto persistentProduto = getProduto(em, produto.getIdProduto());
            checkMovimentoCollection(em, persistentProduto, (Collection<Movimento>) produto.getMovimentoCollection());
            produto = mergeProduto(em, produto);
            mergeMovimentoCollectionToProduto(em, produto);
            em.getTransaction().commit();
        } catch (Exception ex) {
            handleEditException(ex, produto.getIdProduto());
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    private Produto getProduto(EntityManager em, Integer id) throws NonexistentEntityException {
        Produto persistentProduto = em.find(Produto.class, id);
        if (persistentProduto == null) {
            throw new NonexistentEntityException("The produto with id " + id + " no longer exists.");
        }
        return persistentProduto;
    }

    private void checkMovimentoCollection(EntityManager em, Produto persistentProduto, Collection<Movimento> movimentoCollection) throws IllegalOrphanException {
        Collection<Movimento> movimentoCollectionOld = persistentProduto.getMovimentoCollection();
        List<String> illegalOrphanMessages = getIllegalOrphanMessages(movimentoCollectionOld, movimentoCollection);
        if (illegalOrphanMessages != null) {
            throw new IllegalOrphanException(illegalOrphanMessages);
        }
    }

    private Produto mergeProduto(EntityManager em, Produto produto) {
        return em.merge(produto);
    }

    private List<String> getIllegalOrphanMessages(Collection<Movimento> movimentoCollectionOld, Collection<Movimento> movimentoCollectionNew) {
        List<String> illegalOrphanMessages = null;
        for (Movimento movimentoCollectionOldMovimento : movimentoCollectionOld) {
            if (!movimentoCollectionNew.contains(movimentoCollectionOldMovimento)) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<>();
                }
                illegalOrphanMessages.add("You must retain Movimento " + movimentoCollectionOldMovimento + " since its produtoidProduto field is not nullable.");
            }
        }
        return illegalOrphanMessages;
    }

    private void handleEditException(Exception ex, Integer id) throws NonexistentEntityException {
        String msg = ex.getLocalizedMessage();
        if (msg == null || msg.length() == 0) {
            if (findProduto(id) == null) {
                throw new NonexistentEntityException("The produto with id " + id + " no longer exists.");
            }
        }
        throw new NonexistentEntityException("Error editing Produto.", ex);
    }

    public void destroy(Integer id) throws IllegalOrphanException, NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Produto produto = getProduto(em, id);
            checkMovimentoCollection(em, produto, produto.getMovimentoCollection());
            em.remove(produto);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Produto> findProdutoEntities() {
        return findProdutoEntities(true, -1, -1);
    }

    public List<Produto> findProdutoEntities(int maxResults, int firstResult) {
        return findProdutoEntities(false, maxResults, firstResult);
    }

    private List<Produto> findProdutoEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Produto.class));
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

    public Produto findProduto(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Produto.class, id);
        } finally {
            em.close();
        }
    }

    public int getProdutoCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Produto> rt = cq.from(Produto.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
}
