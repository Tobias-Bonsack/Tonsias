package de.tonsias.basis.data.access.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.tonsias.basis.data.access.osgi.impl.SaveServiceImpl;
import de.tonsias.basis.data.access.osgi.intf.SaveService;
import de.tonsias.basis.model.impl.Instanz;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.ISavePathOwner;

public class SaveServiceImplTest {

	private final SaveService _saveService = new SaveServiceImpl();

	@Test
	void testSafeAsGson_writesToPathPlusKey() throws IOException {
		Instanz instanz = new Instanz("save_single");
		instanz.setParentKey("0");
		instanz.addChildKeys("child1", "child2");

		_saveService.safeAsGson(instanz, instanz.getClass());

		Path file = InstanceLocation.resolve("instanz/save_single.json");
		assertThat(Files.exists(file), is(true));

		JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
		assertThat(json.get("_ownKey").getAsString(), is("save_single"));
		assertThat(json.get("_parentKey").getAsString(), is("0"));
	}

	@Test
	void testSafeAsGson_createsMissingFolders() throws IOException {
		SingleStringValue value = new SingleStringValue("save_folders");

		_saveService.safeAsGson(value, value.getClass());

		assertThat(Files.exists(InstanceLocation.resolve("single_value/string/save_folders.json")), is(true));
	}

	@Test
	void testSafeAsGson_biMapIsWrittenAsAPlainObject() throws IOException {
		Instanz instanz = new Instanz("save_bimap");
		instanz.addValuekeys(de.tonsias.basis.model.enums.SingleValueType.SINGLE_STRING,
				java.util.Map.entry("valueKey", "valueName"));

		_saveService.safeAsGson(instanz, instanz.getClass());

		JsonObject json = JsonParser.parseString(Files.readString(InstanceLocation.resolve("instanz/save_bimap.json")))
				.getAsJsonObject();
		JsonObject map = json.getAsJsonObject("_singleStringKeyValueMap");
		assertThat(map.get("valueKey").getAsString(), is("valueName"));
	}

	/**
	 * Overwriting has to replace the file, not just the leading bytes. Currently
	 * the service opens the file with {@code StandardOpenOption.CREATE} only, so
	 * the tail of a longer previous version survives and the file stops being
	 * valid JSON.
	 */
	@Test
	void testSafeAsGson_shorterContentReplacesLongerOne() throws IOException {
		SingleStringValue value = new SingleStringValue("save_overwrite");
		value.tryToSetValue("a very long value that produces a long json document");
		_saveService.safeAsGson(value, value.getClass());

		SingleStringValue shorter = new SingleStringValue("save_overwrite");
		shorter.tryToSetValue("x");
		_saveService.safeAsGson(shorter, shorter.getClass());

		Path file = InstanceLocation.resolve("single_value/string/save_overwrite.json");
		SingleStringValue reloaded = new Gson().fromJson(Files.readString(file), SingleStringValue.class);
		assertThat(reloaded.getValue(), is("x"));
	}

	/** {@code LoadService} reads the files as UTF-8, so they have to be written as UTF-8. */
	@Test
	void testSafeAsGson_nonAsciiSurvivesTheRoundTrip() throws IOException {
		SingleStringValue value = new SingleStringValue("save_umlaut");
		value.tryToSetValue("Grüße, Straße");
		_saveService.safeAsGson(value, value.getClass());

		Path file = InstanceLocation.resolve("single_value/string/save_umlaut.json");
		SingleStringValue reloaded = new Gson().fromJson(Files.readString(file), SingleStringValue.class);

		assertThat(reloaded.getValue(), is("Grüße, Straße"));
	}

	@Test
	void testSafeAsGsonCollection_writesAllElementsUnderTheFirstKey() throws IOException {
		Path folder = InstanceLocation.resolve("instanz");
		Files.createDirectories(folder);
		List<ISavePathOwner> list = List.of(new Instanz("save_coll_1"), new Instanz("save_coll_2"));

		_saveService.safeAsGson(list, Instanz.class);

		Path file = InstanceLocation.resolve("instanz/save_coll_1.json");
		assertThat(Files.exists(file), is(true));

		var array = JsonParser.parseString(Files.readString(file)).getAsJsonArray();
		var keys = array.asList().stream().map(e -> e.getAsJsonObject().get("_ownKey").getAsString()).toList();
		assertThat(keys, containsInAnyOrder("save_coll_1", "save_coll_2"));
	}
}
