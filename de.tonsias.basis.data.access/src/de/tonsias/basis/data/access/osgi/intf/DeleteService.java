package de.tonsias.basis.data.access.osgi.intf;

import java.io.IOException;

public interface DeleteService {

	/**
	 * @param path file path of the object relative to the workspace, the same
	 *             {@code <path><key>.json} {@link SaveService} writes to - a bare
	 *             key denotes no file and deletes nothing
	 */
	boolean deleteFile(String path) throws IOException;
}
