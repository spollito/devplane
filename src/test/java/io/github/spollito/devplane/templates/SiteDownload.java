package io.github.spollito.devplane.templates;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates the example project that the public site's demo offers to download, through
 * the same archiver as the download endpoint. The {@code siteDownload} Gradle task runs
 * it. It lives with the tests so that production code doesn't carry a build tool.
 */
public final class SiteDownload {

	private SiteDownload() {
	}

	/** @param args the path to write the zip to */
	public static void main(String[] args) throws IOException {
		byte[] archive = new ProjectArchiver(new ProjectGenerator("io.github.spollito")).archive(ServiceTemplates.DEFAULT,
				new NewProject("orders-api", "payments-team", "Order management REST API"));
		Path target = Path.of(args[0]);
		Files.createDirectories(target.getParent());
		Files.write(target, archive);
	}

}
