package de.tonsias.basis.model.test.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import de.tonsias.basis.model.impl.Instanz;

public class InstanzTest {

	@Test
	void testGetPath_isTheInstanzFolder() {
		assertThat(new Instanz("0").getPath(), is("instanz/"));
	}

	@Test
	void testEquals_sameKeyIsEqual() {
		assertThat(new Instanz("1"), is(new Instanz("1")));
	}

	@Test
	void testEquals_otherKeyIsNotEqual() {
		assertThat(new Instanz("1"), is(not(new Instanz("2"))));
	}

	@Test
	void testEquals_nullAndForeignTypeAreNotEqual() {
		Instanz instanz = new Instanz("1");

		assertThat(instanz.equals(null), is(false));
		assertThat(instanz.equals("1"), is(false));
	}

	@Test
	void testHashCode_followsTheKey() {
		assertThat(new Instanz("abc").hashCode(), is(new Instanz("abc").hashCode()));
		assertThat(new Instanz("abc").hashCode(), is("abc".hashCode()));
	}

	@Test
	void testToString_containsKeyAndSimpleClassName() {
		assertThat(new Instanz("7").toString(), is("7 : Instanz"));
	}
}
