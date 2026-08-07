package de.tonsias.basis.data.access.osgi.impl;

import java.io.IOException;
import java.nio.file.Files;

import org.osgi.service.component.annotations.Component;

import de.tonsias.basis.data.access.osgi.intf.DeleteService;

@Component
public class DeleteServiceImpl implements DeleteService {

	/**
	 * @param path file path of the object, relative to the workspace like the one
	 *             {@code LoadService} and {@code SaveService} take - e.g.
	 *             {@code instanz/<key>.json}, not the bare key
	 */
	@Override
	public boolean deleteFile(String path) throws IOException {
		Files.delete(InstanceLocationUtil.resolve(path));
		return true;
	}

}
