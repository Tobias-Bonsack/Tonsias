package de.tonsias.basis.data.access.osgi.impl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;

import org.eclipse.core.runtime.Platform;
import org.osgi.service.component.annotations.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import de.tonsias.basis.data.access.osgi.intf.SaveService;
import de.tonsias.basis.model.interfaces.ISavePathOwner;

@Component
public class SaveServiceImpl implements SaveService {

	@Override
	public <E> void safeAsGson(Collection<ISavePathOwner> list, Class<E> objectType) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Type typeToken = new ParameterizedType() {
			@Override
			public Type[] getActualTypeArguments() {
				return new Type[] { objectType };
			}

			@Override
			public Type getOwnerType() {
				return null;
			}

			@Override
			public Type getRawType() {
				return Collection.class;
			}
		};
		String json = gson.toJson(list, typeToken);

		String dir = Platform.getInstanceLocation().getURL().getPath().substring(1);
		String pathName = list.iterator().next().getPath();
		String fileName = list.iterator().next().getOwnKey();
		Path path = Paths.get(dir + File.separator + pathName + fileName + ".json");
		try {
			Files.write(path, json.getBytes(), StandardOpenOption.CREATE);
		} catch (IOException e) {
			Platform.getLog(getClass()).error("Unable to Safe: " + path.toString(), e);
		}
	}

	@Override
	public <E> void safeAsGson(ISavePathOwner object, Class<E> objectType) {
		Gson gson = new Gson();

		var type = TypeToken.getParameterized(objectType).getType();
		String json = gson.toJson(object, type);

		String dir = Platform.getInstanceLocation().getURL().getPath().substring(1);
		String pathName = object.getPath();
		String fileName = object.getOwnKey();
		Path path = Paths.get(dir + File.separator + pathName + fileName + ".json");
		try {
			Files.write(path, json.getBytes(), StandardOpenOption.CREATE);
		} catch (IOException e) {
			Platform.getLog(getClass()).error("Unable to Safe: " + path.toString(), e);
		}
	}
}
