package de.tonsias.basis.data.access.test.system;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.tonsias.basis.data.access.osgi.intf.LoadService;
import de.tonsias.basis.data.access.osgi.intf.SaveService;
import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.interfaces.IMultiValue;
import de.tonsias.basis.model.impl.value.MultiBooleanValue;
import de.tonsias.basis.model.impl.value.MultiFloatValue;
import de.tonsias.basis.model.impl.value.MultiInstanzValue;
import de.tonsias.basis.model.impl.value.MultiIntegerValue;
import de.tonsias.basis.model.impl.value.MultiStringValue;
import de.tonsias.basis.model.impl.value.SingleFloatValue;

/**
 * What a list of values does on the way through Gson.
 * <p>
 * {@code AMultiValue} declares its elements as {@code List<T>}, and the field is
 * erased. Nothing registers an adapter for it: the save service hands over the
 * concrete class, and Gson re-derives the type context from each
 * {@code getGenericSuperclass()} on the way down, so {@code List<T>} resolves to
 * {@code List<Float>} the same way the single value's bare {@code T} resolves to
 * {@code Float}. That is a claim about Gson's internals rather than about this
 * code, which is why it is nailed down here and not reasoned about.
 * </p>
 */
public class MultiValueGsonSystemTest {

	private final LoadService _loadService = RegisteredServices.get(LoadService.class);

	private final SaveService _saveService = RegisteredServices.get(SaveService.class);

	private <T extends IMultiValue<?>> T roundTrip(T value, Class<T> clazz) {
		_saveService.safeAsGson(value, clazz);
		return _loadService.loadFromGson(value.getPath() + value.getOwnKey(), clazz);
	}

	/**
	 * The one that would fail silently: a {@code Double} in the list prints like the
	 * float that went in and compares unequal to it, so the element would be there
	 * and yet never be found again.
	 */
	@Test
	void testRoundTrip_floatElementsComeBackAsFloatsAndNotAsDoubles() {
		MultiFloatValue saved = new MultiFloatValue("mv_float");
		saved.tryToSetValues(List.of(1.5f, -0.5f, 3.14f));
		saved.addConnectedInstanzKey("owner");

		MultiFloatValue loaded = roundTrip(saved, MultiFloatValue.class);

		List<?> elements = loaded.getValues();
		assertThat(elements.get(0), is(instanceOf(Float.class)));
		assertThat(loaded.getValues(), contains(1.5f, -0.5f, 3.14f));
		assertThat(loaded.getConnectedInstanzKeys(), contains("owner"));
	}

	@Test
	void testRoundTrip_integerElementsComeBackAsIntegers() {
		MultiIntegerValue saved = new MultiIntegerValue("mv_int");
		saved.tryToSetValues(List.of(1, 42, -7));

		MultiIntegerValue loaded = roundTrip(saved, MultiIntegerValue.class);

		List<?> elements = loaded.getValues();
		assertThat(elements.get(0), is(instanceOf(Integer.class)));
		assertThat(loaded.getValues(), contains(1, 42, -7));
	}

	@Test
	void testRoundTrip_booleanElementsComeBackAsBooleans() {
		MultiBooleanValue saved = new MultiBooleanValue("mv_bool");
		saved.tryToSetValues(List.of(true, false));

		MultiBooleanValue loaded = roundTrip(saved, MultiBooleanValue.class);

		List<?> elements = loaded.getValues();
		assertThat(elements.get(0), is(instanceOf(Boolean.class)));
		assertThat(loaded.getValues(), contains(true, false));
	}

	/** the order is part of the value, so it has to survive the file */
	@Test
	void testRoundTrip_theOrderIsKept() {
		MultiStringValue saved = new MultiStringValue("mv_order");
		saved.tryToSetValues(List.of("c", "a", "b"));

		MultiStringValue loaded = roundTrip(saved, MultiStringValue.class);

		assertThat(loaded.getValues(), contains("c", "a", "b"));
	}

	@Test
	void testRoundTrip_aRelationKeepsEveryTarget() {
		MultiInstanzValue saved = new MultiInstanzValue("mv_rel");
		saved.tryToSetValues(List.of("1a", "2b"));

		MultiInstanzValue loaded = roundTrip(saved, MultiInstanzValue.class);

		assertThat(loaded.getValues(), contains("1a", "2b"));
	}

	@Test
	void testRoundTrip_anEmptyListSurvives() {
		MultiStringValue saved = new MultiStringValue("mv_empty");

		MultiStringValue loaded = roundTrip(saved, MultiStringValue.class);

		assertThat(loaded.getValues(), is(empty()));
		assertThat(loaded.addValue("later"), is(true));
	}

	/**
	 * Gson allocates without running a constructor, so a field initializer never
	 * runs and a json that names no {@code _values} leaves the field null. The lazy
	 * getter is what keeps that from being a {@code NullPointerException} the first
	 * time anybody looks.
	 */
	@Test
	void testLoadFromGson_aFileWithoutValuesIsAnEmptyListNotNull() throws IOException {
		Path file = InstanceLocation.resolve(MultiValueType.MULTI_STRING.getPath() + "mv_legacy.json");
		Files.createDirectories(file.getParent());
		Files.writeString(file, "{\"_ownKey\":\"mv_legacy\"}");

		MultiStringValue loaded = _loadService.loadFromGson(MultiValueType.MULTI_STRING.getPath() + "mv_legacy",
				MultiStringValue.class);

		assertThat(loaded.getValues(), is(empty()));
		assertThat(loaded.getConnectedInstanzKeys(), is(empty()));
		assertThat(loaded.size(), is(0));
	}

	/**
	 * {@code _ownKey} and {@code _connectedInstanzes} moved out of
	 * {@code ASingleValue} into {@code AValue}. Gson names fields by
	 * {@link java.lang.reflect.Field#getName()} across the whole superclass chain,
	 * so moving one changes neither its name nor its place in the flat json object -
	 * and a file written by 0.2.0 still loads. This is that file, by hand.
	 */
	@Test
	void testLoadFromGson_aSingleValueFileWrittenBeforeAValueExistedStillLoads() throws IOException {
		Path file = InstanceLocation.resolve("single_value/float/mv_pre_avalue.json");
		Files.createDirectories(file.getParent());
		Files.writeString(file,
				"{\"_connectedInstanzes\":[\"owner\"],\"_ownKey\":\"mv_pre_avalue\",\"_value\":2.5}");

		SingleFloatValue loaded = _loadService.loadFromGson("single_value/float/mv_pre_avalue", SingleFloatValue.class);

		assertThat(loaded.getOwnKey(), is("mv_pre_avalue"));
		assertThat(loaded.getValue(), is(2.5f));
		assertThat(loaded.getConnectedInstanzKeys(), contains("owner"));
	}

	/**
	 * The same file without the connection set - the initializer that used to fill
	 * it is skipped by Gson, which made this a {@code NullPointerException} rather
	 * than an empty set.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/83">#83</a>
	 */
	@Test
	void testLoadFromGson_aValueFileWithoutConnectionsIsEmptyNotNull() throws IOException {
		Path file = InstanceLocation.resolve("single_value/float/mv_no_conn.json");
		Files.createDirectories(file.getParent());
		Files.writeString(file, "{\"_ownKey\":\"mv_no_conn\",\"_value\":1.0}");

		SingleFloatValue loaded = _loadService.loadFromGson("single_value/float/mv_no_conn", SingleFloatValue.class);

		assertThat(loaded.getConnectedInstanzKeys(), is(empty()));
		assertThat(loaded.addConnectedInstanzKey("owner"), is(true));
	}
}
