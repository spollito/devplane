package io.github.spollito.devplane.provisioning;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import io.github.spollito.devplane.catalog.RegisteredService;
import io.github.spollito.devplane.catalog.ServiceCatalog;
import io.github.spollito.devplane.catalog.SourceRepository;
import io.github.spollito.devplane.provisioning.GitHubClient.CreatedRepository;
import io.github.spollito.devplane.templates.NewProject;
import io.github.spollito.devplane.templates.ProjectGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

/**
 * Gives a registered service its GitHub repository: generates the project, creates
 * the repository, pushes the project, and records the repository in the catalog.
 * <p>
 * Deliberately not transactional. Each catalog call runs in its own transaction, so
 * none stays open while DevPlane waits for GitHub.
 */
@Service
public class RepositoryProvisioner {

	private static final Logger log = LoggerFactory.getLogger(RepositoryProvisioner.class);

	private final ServiceCatalog catalog;

	private final ProjectGenerator generator;

	private final GitHubClient gitHub;

	private final ProjectPusher pusher;

	private final GitHubProperties properties;

	RepositoryProvisioner(ServiceCatalog catalog, ProjectGenerator generator, GitHubClient gitHub,
			ProjectPusher pusher, GitHubProperties properties) {
		this.catalog = catalog;
		this.generator = generator;
		this.gitHub = gitHub;
		this.pusher = pusher;
		this.properties = properties;
	}

	/**
	 * Provisions the service's repository from a template that the caller has
	 * already checked is listed.
	 * @throws io.github.spollito.devplane.catalog.ServiceNotFoundException if no
	 * service has this name
	 * @throws RepositoryAlreadyProvisionedException if the service has a repository
	 * @throws GitHubNotConfiguredException if no token is configured
	 * @throws RepositoryNameTakenException if the account already has a repository
	 * with the service's name
	 * @throws GitHubException if GitHub or the push fails
	 */
	public SourceRepository provision(String name, String template, boolean isPrivate) {
		RegisteredService service = catalog.lookUp(name);
		if (service.repository() != null) {
			throw new RepositoryAlreadyProvisionedException(name, service.repository().url());
		}
		if (!properties.hasToken()) {
			throw new GitHubNotConfiguredException();
		}
		Path workDirectory = createWorkDirectory();
		try {
			// Generated first, so that a broken template never leaves an empty repository.
			Path project = workDirectory.resolve(service.name());
			generator.generate(template, new NewProject(service.name(), service.owner(), service.description()),
					project);
			CreatedRepository created = gitHub.createRepository(service.name(), service.description(), isPrivate);
			pusher.push(project, created, "Create " + service.name() + " from the " + template + " template");
			SourceRepository repository = catalog.recordRepository(service.name(), created.htmlUrl(), template);
			log.info("Provisioned {} for service {}", repository.url(), service.name());
			return repository;
		}
		finally {
			delete(workDirectory);
		}
	}

	private static Path createWorkDirectory() {
		try {
			return Files.createTempDirectory("devplane-");
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not create a temporary directory", ex);
		}
	}

	private static void delete(Path directory) {
		try {
			// Git writes pack files read-only, and Windows refuses to delete a read-only
			// file, so they're made writable first.
			try (Stream<Path> paths = Files.walk(directory)) {
				paths.forEach(path -> path.toFile().setWritable(true));
			}
			FileSystemUtils.deleteRecursively(directory);
		}
		catch (IOException ex) {
			// Too late to fail the request: the outcome is already decided.
			log.warn("Could not delete temporary directory {}", directory, ex);
		}
	}

}
