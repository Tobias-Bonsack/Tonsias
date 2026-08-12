package de.tonsias.basis.data.access.test.system;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.tonsias.basis.data.access.osgi.intf.LoadService;
import de.tonsias.basis.data.access.osgi.intf.SaveService;
import de.tonsias.basis.model.enums.IValueType;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.enums.ValueTypes;
import de.tonsias.basis.model.impl.Instanz;
import de.tonsias.basis.model.impl.value.SingleFloatValue;
import de.tonsias.basis.model.impl.value.SingleIntegerValue;
import de.tonsias.basis.model.interfaces.IInstanz;

public class LoadServiceSystemTest {

	private final LoadService _loadService = RegisteredServices.get(LoadService.class);

	private final SaveService _saveService = RegisteredServices.get(SaveService.class);

	@Test
	void testLoadFromGson_missingFileIsNull() {
		// the read failure is only logged, callers get null and fall back
		assertThat(_loadService.loadFromGson("instanz/there_is_no_such_key", Instanz.class), is(nullValue()));
	}

	@Test
	void testLoadFromGson_roundTripOfAnInstanz() {
		Instanz saved = new Instanz("load_instanz");
		saved.setParentKey("0");
		saved.addChildKeys("c1", "c2");

		_saveService.safeAsGson(saved, saved.getClass());
		Instanz loaded = _loadService.loadFromGson("instanz/load_instanz", Instanz.class);

		assertThat(loaded.getOwnKey(), is("load_instanz"));
		assertThat(loaded.getParentKey(), is("0"));
		assertThat(loaded.getChildren(), containsInAnyOrder("c1", "c2"));
	}

	/**
	 * The {@code BiMap} fields have no default Gson support - {@code LoadServiceImpl}
	 * registers a deserializer for them, and this is what it is there for.
	 */
	@Test
	void testLoadFromGson_biMapFieldsAreRestored() {
		Instanz saved = new Instanz("load_bimap");
		saved.addValuekeys(SingleValueType.SINGLE_STRING, java.util.Map.entry("sKey", "sName"));
		saved.addValuekeys(SingleValueType.SINGLE_INTEGER, java.util.Map.entry("iKey", "iName"));
		saved.addValuekeys(SingleValueType.SINGLE_BOOLEAN, java.util.Map.entry("bKey", "bName"));
		saved.addValuekeys(SingleValueType.SINGLE_FLOAT, java.util.Map.entry("fKey", "fName"));

		_saveService.safeAsGson(saved, saved.getClass());
		IInstanz loaded = _loadService.loadFromGson("instanz/load_bimap", Instanz.class);

		assertThat(loaded.getValues(SingleValueType.SINGLE_STRING), hasEntry("sKey", "sName"));
		assertThat(loaded.getValues(SingleValueType.SINGLE_INTEGER), hasEntry("iKey", "iName"));
		assertThat(loaded.getValues(SingleValueType.SINGLE_BOOLEAN), hasEntry("bKey", "bName"));
		assertThat(loaded.getValues(SingleValueType.SINGLE_FLOAT), hasEntry("fKey", "fName"));
		// the inverse view is what TreeLabelProvider looks a name up in
		assertThat(loaded.getValues(SingleValueType.SINGLE_STRING).inverse().get("sName"), is("sKey"));
	}

	static java.util.List<IValueType> everyType() {
		return ValueTypes.valuesList();
	}

	/**
	 * A file written before a value type existed carries no map for it, and Gson
	 * constructs the instanz without running any field initializer. Every type still
	 * has to answer with a usable map - {@code TreeNodeWrapper} and
	 * {@code InstanzView} walk all of them and would fail on a null. Every instanz
	 * on disk today is such a file for the five multi types.
	 */
	@ParameterizedTest
	@MethodSource("everyType")
	void testLoadFromGson_mapsMissingFromTheJsonAreEmptyNotNull(IValueType type) throws IOException {
		Path file = InstanceLocation.resolve("instanz/load_legacy.json");
		Files.createDirectories(file.getParent());
		Files.writeString(file, "{\"_ownKey\":\"load_legacy\",\"_parentKey\":\"0\",\"_childKeys\":[]}");

		IInstanz loaded = _loadService.loadFromGson("instanz/load_legacy", Instanz.class);

		assertThat(loaded.getValues(type), is(anEmptyMap()));
	}

	@Test
	void testLoadFromGson_roundTripOfASingleValue() {
		SingleIntegerValue saved = new SingleIntegerValue("load_value");
		saved.tryToSetValue(42);
		saved.addConnectedInstanzKey("owner");

		_saveService.safeAsGson(saved, saved.getClass());
		SingleIntegerValue loaded = _loadService.loadFromGson("single_value/integer/load_value",
				SingleIntegerValue.class);

		assertThat(loaded.getValue(), is(42));
		assertThat(loaded.getConnectedInstanzKeys(), contains("owner"));
	}

	/**
	 * {@code ASingleValue} declares its value as the type variable {@code T}, which
	 * is erased in the field - Gson has to resolve it through the concrete class to
	 * read a JSON number back as a {@code Float} rather than as a {@code Double}.
	 */
	@Test
	void testLoadFromGson_roundTripOfAFloatValue() {
		SingleFloatValue saved = new SingleFloatValue("load_float");
		saved.tryToSetValue(3.14f);

		_saveService.safeAsGson(saved, saved.getClass());
		SingleFloatValue loaded = _loadService.loadFromGson("single_value/float/load_float", SingleFloatValue.class);

		assertThat(loaded.getValue(), is(3.14f));
	}

	@Test
	void testLoadFromGsonArray_readsEveryElement() throws IOException {
		Path file = InstanceLocation.resolve("load_array/list.json");
		Files.createDirectories(file.getParent());
		Files.writeString(file, "[{\"_ownKey\":\"a\"},{\"_ownKey\":\"b\"}]");

		Collection<Instanz> loaded = _loadService.loadFromGsonArray("load_array/list", Instanz.class);

		assertThat(loaded, hasSize(2));
		assertThat(loaded.stream().map(Instanz::getOwnKey).toList(), containsInAnyOrder("a", "b"));
	}

	@Test
	void testLoadFromGsonArray_missingFileIsNull() {
		assertThat(_loadService.loadFromGsonArray("load_array/nope", Instanz.class), is(nullValue()));
	}
}
