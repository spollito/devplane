package io.github.spollito.devplane;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/**
 * The full application as it runs without {@code DEVPLANE_GITHUB_TOKEN}, with MockMvc
 * and a Testcontainers PostgreSQL. The empty property also overrides a token set in
 * the developer's environment.
 * <p>
 * Tests that need no token use exactly this configuration, so they share one cached
 * Spring context, separate from {@link IntegrationTest}'s.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = "devplane.github.token=")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public @interface NoGitHubTokenTest {

}
