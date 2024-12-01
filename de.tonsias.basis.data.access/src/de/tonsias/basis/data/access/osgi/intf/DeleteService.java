package de.tonsias.basis.data.access.osgi.intf;

import java.io.IOException;

public interface DeleteService {

	boolean deleteFile(String path) throws IOException;
}
