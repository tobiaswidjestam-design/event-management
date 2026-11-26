package se.lexicon.eventapp.repository;

import java.util.List;
import java.util.Optional;

/**
 * The storage contract used by the services.
 * Depending on this interface instead of on MySQL is what allows the services to be
 * unit tested with in-memory implementations.
 *
 * @param <T> the type of entity this repository stores
 */
public interface Repository<T> {

    /**
     * Inserts the entity when it has no id yet and updates it otherwise.
     *
     * @return the stored entity, including the generated id
     */
    T save(T entity);

    Optional<T> findById(int id);

    List<T> findAll();

    boolean delete(int id);
}
