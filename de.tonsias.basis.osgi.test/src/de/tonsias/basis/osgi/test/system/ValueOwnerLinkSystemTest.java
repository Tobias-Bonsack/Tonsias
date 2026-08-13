package de.tonsias.basis.osgi.test.system;

import static de.tonsias.basis.osgi.test.ProductRuntime.ROOT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.MultiStringValue;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.IMultiValueService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.test.EventRecorder;
import de.tonsias.basis.osgi.test.ProductRuntime;

/**
 * Both ends of "this instanz holds this value", in the direction that used to do
 * nothing.
 * <p>
 * An attribute hangs on an instanz at both ends: the instanz names the value key,
 * the value names the instanz key. Adding kept those in step from the start;
 * taking one off ran into a {@code // TODO: add logic} and left the value naming
 * an instanz that no longer held it - so the next save wrote that, and a later
 * delete would have told an instanz about a value it did not have.
 * </p>
 *
 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/85">#85</a>
 */
public class ValueOwnerLinkSystemTest {

	IInstanzService _inse;

	ISingleValueService _svs;

	IMultiValueService _mvs;

	IInstanz _first;

	IInstanz _second;

	EventRecorder _recorder;

	@BeforeEach
	void beforeEach() {
		ProductRuntime.start();
		_inse = ProductRuntime.instanzService();
		_svs = ProductRuntime.singleValueService();
		_mvs = ProductRuntime.multiValueService();

		_first = _inse.createInstanz(ROOT, Type.SEND);
		_second = _inse.createInstanz(ROOT, Type.SEND);

		_recorder = EventRecorder.subscribeToAllDeltas(ProductRuntime.broker());
	}

	@AfterEach
	void afterEach() {
		_recorder.unsubscribe();
		ProductRuntime.flushDeltas();
	}

	/** one value, hanging on both instanzen */
	private SingleStringValue sharedValue() {
		SingleStringValue value = _svs.createNew(SingleStringValue.class, _first.getOwnKey(), "shared", "content",
				Type.SEND);
		_svs.addToParent(SingleValueType.SINGLE_STRING, value.getOwnKey(), _second.getOwnKey(), Type.SEND);

		assertThat("both instanzen hold it to begin with", value.getConnectedInstanzKeys(),
				containsInAnyOrder(_first.getOwnKey(), _second.getOwnKey()));
		return value;
	}

	// ---------- from the instanz end ----------

	@Test
	void testRemoveValueKey_theValueStopsNamingThatInstanz() {
		SingleStringValue value = sharedValue();

		_inse.removeValueKey(List.of(_second.getOwnKey()), SingleValueType.SINGLE_STRING, value.getOwnKey(),
				Type.SEND);

		assertThat(value.getConnectedInstanzKeys(), contains(_first.getOwnKey()));
		assertThat(_second.getValues(SingleValueType.SINGLE_STRING).containsKey(value.getOwnKey()), is(false));
		assertThat("the other instanz keeps it", _first.getValues(SingleValueType.SINGLE_STRING),
				org.hamcrest.Matchers.hasKey(value.getOwnKey()));
	}

	/**
	 * The value is left standing even when nobody holds it any more. Unreachable is
	 * not the same as gone, and dropping it is {@code markValueAsDelete}'s decision.
	 */
	@Test
	void testRemoveValueKey_theLastOwnerLeavesTheValueStanding() {
		SingleStringValue value = sharedValue();

		_inse.removeValueKey(List.of(_first.getOwnKey(), _second.getOwnKey()), SingleValueType.SINGLE_STRING,
				value.getOwnKey(), Type.SEND);

		assertThat(value.getConnectedInstanzKeys(), is(empty()));
		assertThat(value.getValue(), is("content"));
		assertThat(_svs.resolveAnyKey(value.getOwnKey()).isPresent(), is(true));
	}

	// ---------- from the value end ----------

	@Test
	void testRemoveFromParent_theInstanzStopsNamingThatValue() {
		SingleStringValue value = sharedValue();

		assertThat(_svs.removeFromParent(SingleValueType.SINGLE_STRING, value.getOwnKey(), _second.getOwnKey(),
				Type.SEND), is(true));

		assertThat(_second.getValues(SingleValueType.SINGLE_STRING).containsKey(value.getOwnKey()), is(false));
		assertThat(value.getConnectedInstanzKeys(), contains(_first.getOwnKey()));
	}

	@Test
	void testRemoveFromParent_worksForAListToo() {
		MultiStringValue value = _mvs.createNew(MultiStringValue.class, _first.getOwnKey(), "shared", List.of("a"),
				Type.SEND);
		_mvs.addToParent(MultiValueType.MULTI_STRING, value.getOwnKey(), _second.getOwnKey(), Type.SEND);

		assertThat(_mvs.removeFromParent(MultiValueType.MULTI_STRING, value.getOwnKey(), _second.getOwnKey(),
				Type.SEND), is(true));

		assertThat(_second.getValues(MultiValueType.MULTI_STRING).containsKey(value.getOwnKey()), is(false));
		assertThat(value.getConnectedInstanzKeys(), contains(_first.getOwnKey()));
		assertThat("the elements are none of this", value.getValues(), contains("a"));
	}

	// ---------- the guards that end the chain ----------

	/**
	 * The two handlers call each other, so a service that fired for a removal that
	 * removed nothing would never stop. Both answer without an event instead.
	 */
	@Test
	void testRemoveFromParent_anInstanzThatDoesNotHoldItFiresNothing() {
		SingleStringValue value = _svs.createNew(SingleStringValue.class, _first.getOwnKey(), "own", "content",
				Type.SEND);
		_recorder.clear();

		assertThat(_svs.removeFromParent(SingleValueType.SINGLE_STRING, value.getOwnKey(), _second.getOwnKey(),
				Type.SEND), is(false));

		assertThat(_recorder.events(), is(empty()));
	}

	@Test
	void testRemoveValueKey_aKeyTheInstanzDoesNotHaveFiresNothing() {
		_recorder.clear();

		_inse.removeValueKey(List.of(_first.getOwnKey()), SingleValueType.SINGLE_STRING, "no-such-key", Type.SEND);

		assertThat(_recorder.events(), is(empty()));
	}

	/**
	 * The chain is {@code removeValueKey -> valueChange -> removeFromParent ->
	 * linkedInstanzChange -> removeValueKey}, and the second time round the key is
	 * gone. Two events for one removal, and no more however often it is walked.
	 */
	@Test
	void testRemovingOneOwner_endsAfterTwoEvents() {
		SingleStringValue value = sharedValue();
		_recorder.clear();

		_inse.removeValueKey(List.of(_second.getOwnKey()), SingleValueType.SINGLE_STRING, value.getOwnKey(),
				Type.SEND);

		assertThat(_recorder.events(), hasSize(lessThanOrEqualTo(2)));
		assertThat(_recorder.topics(),
				containsInAnyOrder("instanz/delta/valueChange", "singleValue/delta/linkedInstanzChange"));
	}

	/** and a second run over the same pair changes nothing and says nothing */
	@Test
	void testRemovingTheSameOwnerTwice_isSilentTheSecondTime() {
		SingleStringValue value = sharedValue();
		_inse.removeValueKey(List.of(_second.getOwnKey()), SingleValueType.SINGLE_STRING, value.getOwnKey(),
				Type.SEND);
		_recorder.clear();

		_inse.removeValueKey(List.of(_second.getOwnKey()), SingleValueType.SINGLE_STRING, value.getOwnKey(),
				Type.SEND);

		assertThat(_recorder.events(), is(empty()));
		assertThat(value.getConnectedInstanzKeys(), contains(_first.getOwnKey()));
	}

	/**
	 * An entry naming a value no file backs can still be got rid of - which is what
	 * the Instanz View's delete on such a row does. Nothing resolves on the way, so
	 * the chain runs out after the first hop.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/86">#86</a>
	 */
	@Test
	void testRemoveValueKey_anEntryWithoutAFileIsStillRemovable() {
		_inse.putValue(_first.getOwnKey(), SingleValueType.SINGLE_STRING, "no-file-key", "orphan", Type.SEND);
		assertThat(_first.getValues(SingleValueType.SINGLE_STRING), org.hamcrest.Matchers.hasKey("no-file-key"));

		_inse.removeValueKey(List.of(_first.getOwnKey()), SingleValueType.SINGLE_STRING, "no-file-key", Type.SEND);

		assertThat(_first.getValues(SingleValueType.SINGLE_STRING).containsKey("no-file-key"), is(false));
		assertThat("and the name is free again",
				_first.getValues(SingleValueType.SINGLE_STRING).inverse().containsKey("orphan"), is(false));
	}

	/**
	 * The delete path cuts the connections itself before firing, so the chain it
	 * starts runs into the same guards and stops rather than coming back for a
	 * second round.
	 */
	@Test
	void testMarkValueAsDelete_stillLeavesBothEndsEmpty() {
		SingleStringValue value = sharedValue();

		_svs.markValueAsDelete(value.getOwnKey(), Type.SEND);

		assertThat(value.getConnectedInstanzKeys(), is(empty()));
		assertThat(_first.getValues(SingleValueType.SINGLE_STRING).containsKey(value.getOwnKey()), is(false));
		assertThat(_second.getValues(SingleValueType.SINGLE_STRING).containsKey(value.getOwnKey()), is(false));
	}
}
