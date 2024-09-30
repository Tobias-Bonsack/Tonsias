package de.tonsias.basis.osgi.test.impl;

import static de.tonsias.basis.osgi.impl.BasicPreferenceServiceImpl.REGEX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tonsias.basis.osgi.impl.BasicPreferenceServiceImpl;

@ExtendWith(MockitoExtension.class)
public class BasicPreferenceServiceImplTest {

	@Mock
	IEclipsePreferences _node;

	@InjectMocks
	BasicPreferenceServiceImplTestee _basicPref;

	@ParameterizedTest
	@MethodSource
	void testGetAsList(String value, Class<?> type) {
		doReturn(value).when(_node).get(anyString(), any());

		List<?> asList = _basicPref.getAsList("any", type);
		assertThat(asList.isEmpty(), is(false));
		assertThat(asList.iterator().next(), isA(type));
		assertThat(asList.size(), is(value.split(REGEX).length));
	}

	private static Stream<Arguments> testGetAsList() {
		List<Arguments> arguments = new ArrayList<>();

		arguments.add(arguments(String.format("t%sDD%sAA", REGEX, REGEX, REGEX), String.class));
		arguments.add(arguments(String.format("1%s2%s3", REGEX, REGEX, REGEX), Integer.class));

		return arguments.stream();
	}

	@ParameterizedTest
	@MethodSource
	void testGetValue(String value, Class<?> type) {
		doReturn(value).when(_node).get(anyString(), any());

		List<?> asList = _basicPref.getAsList("any", type);
		assertThat(asList.isEmpty(), is(false));
		assertThat(asList.iterator().next(), isA(type));
		assertThat(asList.size(), is(value.split(REGEX).length));
	}

	private static Stream<Arguments> testGetValue() {
		List<Arguments> arguments = new ArrayList<>();

		arguments.add(arguments(String.format("tttt", REGEX, REGEX, REGEX), String.class));
		arguments.add(arguments(String.format("1235", REGEX, REGEX, REGEX), Integer.class));

		return arguments.stream();
	}

	private static class BasicPreferenceServiceImplTestee extends BasicPreferenceServiceImpl {
		private IEclipsePreferences _node;

		private BasicPreferenceServiceImplTestee(IEclipsePreferences node) {
			super();
			_node = node;
		}

		@Override
		protected IEclipsePreferences getNode() {
			return _node;
		}

	}

}
