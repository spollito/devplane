package io.github.spollito.devplane.catalog;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {

	Optional<ServiceEntity> findByName(String name);

	Page<ServiceEntity> findByOwner(String owner, Pageable pageable);

	long countByOwner(String owner);

}
