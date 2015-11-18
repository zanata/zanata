// Implementation copied from Seam 2.3.1

package org.zanata.seam.framework;

import javax.persistence.EntityManager;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.transaction.Transaction;
import org.zanata.model.ModelEntityBase;
import org.zanata.util.ServiceLocator;

import static javax.transaction.Status.STATUS_ACTIVE;
import static javax.transaction.Status.STATUS_MARKED_ROLLBACK;

/**
 * Base class for Home objects of JPA entities.
 *
 * @author Gavin King
 */
public class EntityHome<E> extends Home<EntityManager, E> {
    private static final long serialVersionUID = -3140094990727574632L;

    /**
     * Run on {@link EntityHome} instantiation. <br />
     * Validates that an {@link EntityManager} is available.
     *
     * @see Home#create()
     */
    @Override
    public void create() {
        super.create();
        if (getEntityManager() == null) {
            throw new IllegalStateException("entityManager is null");
        }
    }

    /**
     * Returns true if the entity instance is managed
     */
    @Transactional
    public boolean isManaged() {
        return getInstance() != null &&
                getEntityManager().contains(getInstance());
    }

    /**
     * Flush any changes made to the managed entity instance to the underlying
     * database. <br />
     * If the update is successful, a log message is printed, a
     * {@link javax.faces.application.FacesMessage} is added and a transaction
     * success event raised.
     *
     * @return "updated" if the update is successful
     */
    @Transactional
    public String update() {
        joinTransaction();
        getEntityManager().flush();
        updatedMessage();
        return "updated";
    }

    /**
     * Persist unmanaged entity instance to the underlying database. If the
     * persist is successful, a log message is printed, a
     * {@link javax.faces.application.FacesMessage } is added and a transaction
     * success event raised.
     *
     * @return "persisted" if the persist is successful
     */
    @Transactional
    public String persist() {
        getEntityManager().persist(getInstance());
        getEntityManager().flush();
        E instance = getInstance();
        if (instance instanceof ModelEntityBase) {
            Long id = ((ModelEntityBase) instance).getId();
            assignId(id);
        } else {
            // this should not happen. We don't have an EntityHome<E> where E is
            // not a subclass of ModelEntityBase
            throw new RuntimeException("can not get id from entity");
        }

        createdMessage();
        return "persisted";
    }

    /**
     * Remove managed entity instance from the Persistence Context and the
     * underlying database. If the remove is successful, a log message is
     * printed, a {@link javax.faces.application.FacesMessage} is added and a
     * transaction success event raised.
     *
     * @return "removed" if the remove is successful
     */
    @Transactional
    public String remove() {
        getEntityManager().remove(getInstance());
        getEntityManager().flush();
        deletedMessage();
        return "removed";
    }

    /**
     * Implementation of {@link Home#find() find()} for JPA
     *
     * @see Home#find()
     */
    @Transactional
    @Override
    public E find() {
        if (getEntityManager().isOpen()) {
            E result = loadInstance();
            if (result == null) {
                result = handleNotFound();
            }
            return result;
        } else {
            return null;
        }
    }

    /**
     * Utility method to load entity instance from the {@link EntityManager}.
     * Called by {@link #find()}. <br />
     * Can be overridden to support eager fetching of associations.
     *
     * @return The entity identified by {@link Home#getEntityClass()
     *         getEntityClass()}, {@link Home#getId() getId()}
     */
    protected E loadInstance() {
        return getEntityManager().find(getEntityClass(), getId());
    }

    /**
     * Implementation of {@link Home#joinTransaction() joinTransaction()} for
     * JPA.
     */
    @Override
    protected void joinTransaction() {
        if (getEntityManager().isOpen()) {
            try {
                UserTransaction transaction = getTransaction();
                int status = transaction.getStatus();
                boolean isActiveOrMarkedRollback = status == STATUS_ACTIVE || status == STATUS_MARKED_ROLLBACK;
                if (isActiveOrMarkedRollback) {
                    getEntityManager().joinTransaction();
                }
            } catch (SystemException se) {
                throw new RuntimeException("could not join transaction", se);
            }
        }
    }

    // TODO [CDI] inject UserTransaction
    private UserTransaction getTransaction() {
        return Transaction.instance();
    }

    /**
     * The Seam Managed Persistence Context used by this Home component
     */
    public EntityManager getEntityManager() {
        return ServiceLocator.instance().getEntityManager();
    }

}
