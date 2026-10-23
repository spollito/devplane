package io.github.spollito.devplane.catalog;

import java.net.URI;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The catalog's business rules: registering services and keeping their details.
 * <p>
 * Other features change services only through this class, and see them as the
 * read-only {@link RegisteredService}. The JPA entity never leaves it.
 */
@Service
@Transactional
public class ServiceCatalog {

	/** PostgreSQL's SQLSTATE for a unique constraint violation. */
	private static final String UNIQUE_VIOLATION = "23505";

	private final ServiceRepository repository;

	private final Clock clock;

	ServiceCatalog(ServiceRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	/**
	 * Registers a new service, defaulting its lifecycle to experimental.
	 * @throws DuplicateServiceNameException if a service with this name exists
	 */
	public RegisteredService register(String name, String owner, @Nullable String description,
			@Nullable Lifecycle lifecycle) {
		ServiceEntity service = new ServiceEntity(name, owner, description,
				(lifecycle != null) ? lifecycle : Lifecycle.EXPERIMENTAL, now());
		try {
			return RegisteredService.from(repository.saveAndFlush(service));
		}
		catch (DataIntegrityViolationException ex) {
			// The name's unique constraint is the only one an insert can violate, and
			// relying on it (rather than checking first) also holds when requests race.
			if (ex.getMostSpecificCause() instanceof SQLException sql && UNIQUE_VIOLATION.equals(sql.getSQLState())) {
				throw new DuplicateServiceNameException(name, ex);
			}
			throw ex;
		}
	}

	/**
	 * Returns the service with the given name.
	 * @throws ServiceNotFoundException if no service has this name
	 */
	@Transactional(readOnly = true)
	ServiceEntity get(String name) {
		return repository.findByName(name).orElseThrow(() -> new ServiceNotFoundException(name));
	}

	/**
	 * Returns the service with the given name, for other DevPlane features.
	 * @throws ServiceNotFoundException if no service has this name, which the API
	 * renders as 404 wherever it's thrown
	 */
	@Transactional(readOnly = true)
	public RegisteredService lookUp(String name) {
		return RegisteredService.from(get(name));
	}

	/**
	 * Records the GitHub repository provisioned for a service, for other DevPlane
	 * features. The repository's creation time is also the service's new
	 * {@code updatedAt}.
	 * @throws ServiceNotFoundException if no service has this name
	 * @throws IllegalStateException if the service already has a repository
	 */
	public SourceRepository recordRepository(String name, URI url, String template) {
		ServiceEntity service = get(name);
		service.recordRepository(url, template, now());
		return service.getRepository();
	}

	/** Lists services one page at a time, optionally only those with the given owner. */
	@Transactional(readOnly = true)
	public Page<RegisteredService> list(@Nullable String owner, Pageable pageable) {
		Page<ServiceEntity> services = (owner != null) ? repository.findByOwner(owner, pageable)
				: repository.findAll(pageable);
		return services.map(RegisteredService::from);
	}

	/**
	 * Replaces a service's owner, description, and lifecycle. The name never changes.
	 * @throws ServiceNotFoundException if no service has this name
	 */
	RegisteredService update(String name, String owner, @Nullable String description, Lifecycle lifecycle) {
		ServiceEntity service = get(name);
		service.update(owner, description, lifecycle, now());
		return RegisteredService.from(service);
	}

	/**
	 * Removes a service from the catalog. Its GitHub repository, if any, stays on
	 * GitHub.
	 * @throws ServiceNotFoundException if no service has this name
	 */
	public void unregister(String name) {
		repository.delete(get(name));
	}

	/**
	 * The current time at the database's precision. PostgreSQL stores microseconds, so
	 * finer precision would make a saved service differ from the same service read back.
	 */
	private Instant now() {
		return clock.instant().truncatedTo(ChronoUnit.MICROS);
	}

}
