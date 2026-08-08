package de.tonsias.basis.data.access.test.system;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import de.tonsias.basis.data.access.osgi.intf.DeleteService;

public class DeleteServiceSystemTest {

	private final DeleteService _deleteService = RegisteredServices.get(DeleteService.class);

	@Test
	void testDeleteFile_existingFileIsGone() throws IOException {
		Path file = InstanceLocation.resolve("delete_test/existing.json");
		Files.createDirectories(file.getParent());
		Files.writeString(file, "{}");

		boolean deleted = _deleteService.deleteFile(file.toString());

		assertThat(deleted, is(true));
		assertThat(Files.exists(file), is(false));
	}

	/**
	 * A relative path is resolved against the workspace, the same way
	 * {@code SaveService} writes it - that is what the services hand over.
	 */
	@Test
	void testDeleteFile_relativePathIsResolvedAgainstTheWorkspace() throws IOException {
		Path file = InstanceLocation.resolve("delete_test/relative.json");
		Files.createDirectories(file.getParent());
		Files.writeString(file, "{}");

		assertThat(_deleteService.deleteFile("delete_test/relative.json"), is(true));

		assertThat(Files.exists(file), is(false));
	}

	/**
	 * The service takes the file path of an object, not its key - a bare key
	 * denotes no file and deletes nothing.
	 */
	@Test
	void testDeleteFile_unknownPathThrows() {
		assertThrows(NoSuchFileException.class, () -> _deleteService.deleteFile("a_key_is_not_a_path"));
	}

	@Test
	void testDeleteFile_directoryPathThrows() throws IOException {
		Path dir = InstanceLocation.resolve("delete_test/a_folder");
		Files.createDirectories(dir);
		Files.writeString(dir.resolve("child.json"), "{}");

		assertThrows(IOException.class, () -> _deleteService.deleteFile(dir.toString()));
	}
}
