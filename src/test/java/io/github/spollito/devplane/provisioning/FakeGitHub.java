package io.github.spollito.devplane.provisioning;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jspecify.annotations.Nullable;
import org.springframework.util.FileSystemUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * A stand-in for the one GitHub REST API endpoint DevPlane calls,
 * {@code POST /user/repos}, served from the test JVM.
 * <p>
 * Like GitHub, it checks the bearer token, rejects a name that's already taken, and
 * creates the repository. The repository is a bare Git repository in a temporary
 * directory, and the response's {@code clone_url} is its {@code file:} URI, so
 * DevPlane's push lands somewhere tests can clone it back from. Tests can also make
 * it fail in the ways GitHub can.
 */
public final class FakeGitHub implements AutoCloseable {

	public static final String TOKEN = "test-token-5f2c";

	public static final String OWNER_LOGIN = "octo-dev";

	public static final long OWNER_ID = 7654321;

	private static final String NAME_TAKEN = """
			{"message": "Repository creation failed.", "errors": [{"resource": "Repository", "code": "custom",
			"field": "name", "message": "name already exists on this account"}], "status": "422"}
			""";

	private final JsonMapper json = JsonMapper.builder().build();

	private final HttpServer server;

	private final Path root;

	private final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();

	private final Map<String, FakeRepository> repositories = new ConcurrentHashMap<>();

	private final AtomicInteger requestIds = new AtomicInteger();

	private volatile @Nullable Response failure;

	private volatile boolean brokenPushes;

	private FakeGitHub(HttpServer server, Path root) {
		this.server = server;
		this.root = root;
		server.createContext("/", this::handle);
	}

	public static FakeGitHub start() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
			FakeGitHub gitHub = new FakeGitHub(server, Files.createTempDirectory("fake-github-"));
			server.start();
			return gitHub;
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/** The API root to configure as {@code devplane.github.api-url}. */
	public URI url() {
		return URI.create("http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort());
	}

	/** Every request received since the last {@link #reset()}, in order. */
	public List<RecordedRequest> requests() {
		return List.copyOf(requests);
	}

	public Optional<FakeRepository> repository(String name) {
		return Optional.ofNullable(repositories.get(name));
	}

	/** Creates a repository as if someone had created it on GitHub directly. */
	public FakeRepository addExistingRepository(String name) {
		return createRepository(name, null, true, false);
	}

	/** Answers every following create request with this status and body. */
	public void respondWith(int status, String body) {
		this.failure = new Response(status, body);
	}

	/** Creates repositories as usual, but returns clone URLs that can't be pushed to. */
	public void breakPushes() {
		this.brokenPushes = true;
	}

	/** Forgets all requests, repositories, and failures. */
	public void reset() {
		requests.clear();
		repositories.clear();
		failure = null;
		brokenPushes = false;
		try {
			delete(root);
			Files.createDirectories(root);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@Override
	public void close() throws IOException {
		server.stop(0);
		delete(root);
	}

	/**
	 * Git writes pack files read-only, and Windows refuses to delete a read-only
	 * file, so they're made writable first.
	 */
	private static void delete(Path directory) throws IOException {
		if (Files.exists(directory)) {
			try (Stream<Path> paths = Files.walk(directory)) {
				paths.forEach(path -> path.toFile().setWritable(true));
			}
		}
		FileSystemUtils.deleteRecursively(directory);
	}

	private void handle(HttpExchange exchange) throws IOException {
		try (exchange) {
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			Map<String, String> headers = new TreeMap<>();
			exchange.getRequestHeaders()
				.forEach((name, values) -> headers.put(name.toLowerCase(Locale.ROOT), String.join(", ", values)));
			String path = exchange.getRequestURI().getPath();
			requests.add(new RecordedRequest(exchange.getRequestMethod(), path, headers, body));
			Response response = respond(exchange.getRequestMethod(), path, headers, body);
			byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
			exchange.getResponseHeaders()
				.set("X-GitHub-Request-Id", "FAKE:%04d".formatted(requestIds.incrementAndGet()));
			exchange.sendResponseHeaders(response.status(), bytes.length);
			exchange.getResponseBody().write(bytes);
		}
	}

	private Response respond(String method, String path, Map<String, String> headers, String body) {
		if (!"POST".equals(method) || !"/user/repos".equals(path)) {
			return new Response(404, "{\"message\": \"Not Found\"}");
		}
		if (!("Bearer " + TOKEN).equals(headers.get("authorization"))) {
			return new Response(401, "{\"message\": \"Bad credentials\", \"status\": \"401\"}");
		}
		Response failure = this.failure;
		if (failure != null) {
			return failure;
		}
		Map<?, ?> request = json.readValue(body, Map.class);
		String name = (String) request.get("name");
		if (repositories.containsKey(name)) {
			return new Response(422, NAME_TAKEN);
		}
		FakeRepository repository = createRepository(name, (String) request.get("description"),
				Boolean.TRUE.equals(request.get("private")), brokenPushes);
		Map<String, Object> owner = Map.of("login", OWNER_LOGIN, "id", OWNER_ID);
		return new Response(201,
				json.writeValueAsString(Map.of("name", name, "full_name", OWNER_LOGIN + "/" + name, "private",
						repository.isPrivate(), "html_url", repository.htmlUrl().toString(), "clone_url",
						repository.cloneUrl().toString(), "default_branch", "main", "owner", owner)));
	}

	private FakeRepository createRepository(String name, @Nullable String description, boolean isPrivate,
			boolean unreachable) {
		Path directory = root.resolve(name + ".git");
		try (Git git = Git.init().setBare(true).setInitialBranch("main").setDirectory(directory.toFile()).call()) {
			// Only creating it.
		}
		catch (GitAPIException ex) {
			throw new IllegalStateException(ex);
		}
		URI cloneUrl = unreachable ? root.resolve("missing").resolve(name + ".git").toUri() : directory.toUri();
		FakeRepository repository = new FakeRepository(name, description, isPrivate, directory, cloneUrl);
		repositories.put(name, repository);
		return repository;
	}

	/** A request as the fake received it. Header names are lowercase. */
	public record RecordedRequest(String method, String path, Map<String, String> headers, String body) {

	}

	/**
	 * A repository the fake created.
	 *
	 * @param directory the bare Git repository, for tests to clone
	 * @param cloneUrl what the fake told DevPlane to push to
	 */
	public record FakeRepository(String name, @Nullable String description, boolean isPrivate, Path directory,
			URI cloneUrl) {

		public URI htmlUrl() {
			return URI.create("https://github.com/" + OWNER_LOGIN + "/" + name);
		}

	}

	private record Response(int status, String body) {

	}

}
