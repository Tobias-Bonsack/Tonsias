package de.tonsias.basis.model.test.value;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleInstanzValue;

public class SingleInstanzValueTest {

	/**
	 * A fresh reference points nowhere. The empty string is deliberately not a key
	 * {@code accepts} would take, so the dialog keeps its OK button off until an
	 * instanz has been chosen.
	 */
	@Test
	void testNewValue_pointsNowhere() {
		assertThat(new SingleInstanzValue("k").getValue(), is(""));
		assertThat(SingleInstanzValue.accepts(new SingleInstanzValue("k").getValue()), is(false));
	}

	@Test
	void testGetPath_matchesTheEnum() {
		assertThat(new SingleInstanzValue("k").getPath(), is(SingleValueType.SINGLE_INSTANZ.getPath()));
	}

	/**
	 * The value folder sits below {@code single_value/}, so it cannot meet the
	 * {@code instanz/} folder the instanzen themselves are written into - both would
	 * otherwise hold a file named after the same key.
	 */
	@Test
	void testGetPath_doesNotCollideWithTheInstanzFolder() {
		assertThat(SingleValueType.SINGLE_INSTANZ.getPath(), is("single_value/instanz/"));
	}

	@Test
	void testTryToSetValue_key() {
		SingleInstanzValue value = new SingleInstanzValue("k");

		assertThat(value.tryToSetValue("3"), is(true));
		assertThat(value.getValue(), is("3"));
	}

	@Test
	void testTryToSetValue_sameKeyIsNoChange() {
		SingleInstanzValue value = new SingleInstanzValue("k");
		value.tryToSetValue("3");

		assertThat(value.tryToSetValue("3"), is(false));
	}

	/**
	 * {@code accepts} is what the dialog asks before it offers its OK button, so it
	 * has to answer for the same input the same way {@code tryToSetValue} does.
	 * Keys are base 36 and lower case only - upper case is out because the files are
	 * named after them and two of them would be one file on Windows.
	 */
	@ParameterizedTest
	@ValueSource(strings = { "0", "3", "z", "1a", "10", "zzz" })
	void testAccepts_agreesWithTryToSetValue_onWhatItTakes(String input) {
		assertThat(SingleInstanzValue.accepts(input), is(true));
		// seeded away from every input, because setValue answers false for "already
		// that value" as well
		assertThat(new SingleInstanzValue("k", "seed", Set.of()).tryToSetValue(input), is(true));
	}

	/**
	 * @see #testAccepts_agreesWithTryToSetValue_onWhatItTakes(String)
	 * @see #testTryToSetValue_theEmptyStringPointsNowhereAndIsTheOneDisagreement()
	 *      for the single input the two answer differently
	 */
	@ParameterizedTest
	@ValueSource(strings = { " ", "A", "1A", "-1", "1 2", "1.2", "a b", " 3" })
	void testAccepts_agreesWithTryToSetValue_onWhatItRejects(String input) {
		assertThat(SingleInstanzValue.accepts(input), is(false));
		assertThat(new SingleInstanzValue("k", "seed", Set.of()).tryToSetValue(input), is(false));
	}

	/**
	 * The two questions come apart on exactly one input. Pointing nowhere is a
	 * state this value has - the one a fresh one starts in - and a relation is put
	 * back into it when its target is deleted, so {@code tryToSetValue} has to take
	 * the empty string. {@code accepts} still refuses it, because that is what the
	 * dialog asks: nothing chosen must not become a value.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/74">#74</a>
	 */
	@Test
	void testTryToSetValue_theEmptyStringPointsNowhereAndIsTheOneDisagreement() {
		SingleInstanzValue value = new SingleInstanzValue("k", "seed", Set.of());

		assertThat(value.tryToSetValue(""), is(true));
		assertThat(value.getValue(), is(""));
		assertThat("but never on offer in the dialog", SingleInstanzValue.accepts(""), is(false));
		assertThat("and no change when it already points nowhere", value.tryToSetValue(""), is(false));
	}

	@Test
	void testAccepts_null() {
		assertThat(SingleInstanzValue.accepts(null), is(false));
	}

	/**
	 * Anything that is not text is not a key either - the value travels from the
	 * widget as a {@link String} and from nowhere else.
	 */
	@Test
	void testTryToSetValue_nonTextIsRejected() {
		SingleInstanzValue value = new SingleInstanzValue("k", "seed", Set.of());

		assertThat("a number is no key", value.tryToSetValue(3), is(false));
		assertThat("nothing is no key", value.tryToSetValue(null), is(false));
		assertThat(value.getValue(), is("seed"));
	}

	@Test
	void testToString_containsKeyValueAndSimpleClassName() {
		SingleInstanzValue value = new SingleInstanzValue("k");
		value.tryToSetValue("3");

		assertThat(value.toString(), is("k 3 : SingleInstanzValue"));
	}
}
