package de.tonsias.basis.data.access.osgi.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.osgi.service.component.annotations.Component;

import de.tonsias.basis.data.access.osgi.intf.DeleteService;

@Component
public class DeleteServiceImpl implements DeleteService {

	@Override
	public boolean deleteFile(String path) throws IOException {
		Files.delete(Paths.get(path));
		return true;
	}

}
