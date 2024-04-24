package de.tonsias.basis.data.access.osgi.impl;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.osgi.service.component.annotations.Component;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import de.tonsias.basis.data.access.osgi.intf.LoadService;

@Component
public class LoadServiceImpl implements LoadService {

	private final Gson GSON = createGson();

	private String getJson(String path) {
		String dir = Platform.getInstanceLocation().getURL().getPath().substring(1);
		String json = "";
		Path pathToJson = Paths.get(dir + path + ".json");
		try {
			json = Files.readString(pathToJson);
		} catch (IOException e) {
			Platform.getLog(getClass()).error("Can not load: " + path, e);
		}
		return json;
	}

	@Override
	public <E> E loadFromGson(String path, Class<E> objectType) {
		String json = getJson(path);

		E loadedObject = GSON.fromJson(json, objectType);
		return loadedObject;
	}

	@Override
	public <E> Collection<E> loadFromGsonArray(String path, Class<E> objectType) {
		String json = getJson(path);
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

		Collection<E> loadedColl = GSON.fromJson(json, typeToken);
		return loadedColl;
	}
	
    private Gson createGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(
            new TypeToken<BiMap<String, String>>() {}.getType(),
            (JsonDeserializer<BiMap<String, String>>) (json, typeOfT, context) -> {
                JsonObject jsonObject = json.getAsJsonObject();
                BiMap<String, String> map = HashBiMap.create();
                for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                    map.put(entry.getKey(), entry.getValue().getAsString());
                }
                return map;
            }
        );
        return gsonBuilder.create();
    }
}
