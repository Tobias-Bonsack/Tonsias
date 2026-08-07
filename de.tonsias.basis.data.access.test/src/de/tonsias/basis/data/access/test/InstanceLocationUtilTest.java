package de.tonsias.basis.data.access.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.Platform;
import org.junit.jupiter.api.Test;

import de.tonsias.basis.data.access.osgi.impl.InstanceLocationUtil;

public class InstanceLocationUtilTest {

	/**
	 * The instance location is a URL whose path carries a leading slash on Windows
	 * ({@code /C:/ws}) but is the root itself everywhere else ({@code /home/u/ws}).
	 * Cutting that first character turns the workspace into a relative path on
	 * every non-Windows platform.
	 */
	@Test
	void testGetDirectory_isTheInstanceLocationAsAFileSystemPath() {
		Path dir = InstanceLocationUtil.getDirectory();

		assertThat(dir.isAbsolute(), is(true));
		assertThat(dir, is(new File(Platform.getInstanceLocation().getURL().getFile()).getAbsoluteFile().toPath()));
	}

	@Test
	void testResolve_relativePathStaysInTheWorkspace() {
		Path resolved = InstanceLocationUtil.resolve("instanz/key.json");

		assertThat(resolved.isAbsolute(), is(true));
		assertThat(resolved, is(InstanceLocationUtil.getDirectory().resolve(Paths.get("instanz", "key.json"))));
	}

	@Test
	void testResolve_absolutePathIsTakenAsIs() {
		Path absolute = InstanceLocationUtil.getDirectory().resolve("elsewhere.json").toAbsolutePath();

		assertThat(InstanceLocationUtil.resolve(absolute.toString()), is(absolute));
	}
}
