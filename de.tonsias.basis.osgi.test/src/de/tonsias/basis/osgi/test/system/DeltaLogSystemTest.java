package de.tonsias.basis.osgi.test.system;

import static de.tonsias.basis.osgi.test.ProductRuntime.ROOT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Map;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.service.event.Event;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.InstanzEvent;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants;
import de.tonsias.basis.osgi.test.ProductRuntime;

/**
 * The log the registered {@code DeltaServiceImpl} keeps - what goes into it,
 * what stays out, and what is left after a save.
 * <p>
 * The log is the only record of what still needs writing, and the Delta view
 * renders it as it stands. So an event that never arrives is a file that is
 * never saved, and an event that arrives although nobody should have heard it
 * is a row in a view that should not be there. What those recorded events then
 * do to the files is {@link DeltaPersistenceSystemTest}.
 * </p>
 */
public class DeltaLogSystemTest {

	IInstanzService _inse;

	ISingleValueService _svs;

	IDeltaService _delta;

	IEventBrokerBridge _broker;

	@BeforeEach
	void beforeEach() {
		ProductRuntime.start();
		_inse = ProductRuntime.instanzService();
		_svs = ProductRuntime.singleValueService();
		_delta = ProductRuntime.deltaService();
		_broker = ProductRuntime.broker();

		ProductRuntime.flushDeltas();
	}

	@AfterEach
	void afterEach() {
		ProductRuntime.flushDeltas();
	}

	private List<String> loggedTopics() {
		return _delta.getDeltas().stream().map(Event::getTopic).toList();
	}

	/**
	 * A fresh log holds nothing but the start event, which is also the root row of
	 * the Delta view.
	 */
	@Test
	void testLog_startsWithTheStartEventAlone() {
		assertThat(_delta.getDeltas(), contains(IDeltaService.START_EVENT));
	}

	/**
	 * The service subscribes to two wildcard topics, so every delta any service
	 * fires has to land in the log without the service knowing about it.
	 */
	@Test
	void testLog_collectsEveryInstanzAndSingleValueDeltaOfAChain() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		_svs.createNew(SingleStringValue.class, owner.getOwnKey(), "parameter", "content", Type.SEND);

		assertThat(loggedTopics().stream().skip(1).toList(),
				containsInAnyOrder(InstanzEventConstants.NEW, InstanzEventConstants.CHILD_LIST_CHANGE,
						SingleValueEventConstants.NEW, InstanzEventConstants.VALUE_LIST_CHANGE));
	}

	@Test
	void testLog_keepsEveryEventInTheOrderItArrived() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		_svs.createNew(SingleStringValue.class, owner.getOwnKey(), "parameter", "content", Type.SEND);

		List<String> topics = loggedTopics();
		assertThat(topics.get(0), is(IDeltaService.START_TOPIC));
		// the instanz was created first, so both of its events precede both of the
		// value's - within one call the order is up to the event admin
		assertThat(topics.subList(1, 3),
				containsInAnyOrder(InstanzEventConstants.NEW, InstanzEventConstants.CHILD_LIST_CHANGE));
		assertThat(topics.subList(3, 5),
				containsInAnyOrder(SingleValueEventConstants.NEW, InstanzEventConstants.VALUE_LIST_CHANGE));
	}

	/**
	 * The brackets carry no payload and describe no change; they are recorded only
	 * so the Delta view can group what happened between them.
	 */
	@Test
	void testLog_recordsTheOperationBracketsAroundTheDeltas() {
		_broker.send(EventConstants.OPEN_OPERATION, null);
		_inse.createInstanz(ROOT, Type.SEND);
		_broker.send(EventConstants.CLOSE_OPERATION, null);

		List<String> topics = loggedTopics();
		assertThat(topics.get(1), is(EventConstants.OPEN_OPERATION));
		assertThat(topics.get(topics.size() - 1), is(EventConstants.CLOSE_OPERATION));
		assertThat(topics, hasItem(InstanzEventConstants.NEW));
	}

	/**
	 * Selecting an instanz changes nothing about the model, and the topic sits
	 * outside the delta wildcard so the log never hears about it.
	 */
	@Test
	void testLog_ignoresTopicsOutsideTheDeltaWildcard() {
		_broker.send(InstanzEventConstants.SELECTED, Map.of(IEventBroker.DATA, new InstanzEvent(ROOT, null)));

		assertThat(_delta.getDeltas(), contains(IDeltaService.START_EVENT));
	}

	/**
	 * A topic under the wildcard that is not registered in {@code KNOWN_DELTA} is
	 * still recorded - it shows up in the Delta view - but the save folds it into
	 * nothing rather than failing over it.
	 */
	@Test
	void testSaveDeltas_anUnregisteredDeltaTopicIsRecordedAndThenIgnored() {
		_broker.send(InstanzEventConstants.INSTANZ + "/delta/notRegistered", Map.of(IEventBroker.DATA, "whatever"));
		assertThat(loggedTopics(), hasItem(InstanzEventConstants.INSTANZ + "/delta/notRegistered"));

		_delta.saveDeltas();

		assertThat(_delta.getDeltas(), contains(IDeltaService.START_EVENT));
	}

	@Test
	void testSaveDeltas_resetsTheLogToTheStartEvent() {
		_inse.createInstanz(ROOT, Type.SEND);
		assertThat(_delta.getDeltas(), hasSize(greaterThan(1)));

		_delta.saveDeltas();

		assertThat(_delta.getDeltas(), contains(IDeltaService.START_EVENT));
	}

	@Test
	void testSaveDeltas_onAnEmptyLogIsANoOp() {
		_delta.saveDeltas();

		assertThat(_delta.getDeltas(), contains(IDeltaService.START_EVENT));
	}

	/**
	 * {@code SAVE_ALL} is the marker the toolbar handler sends; it has to reach the
	 * same save the Delta view's own button reaches.
	 */
	@Test
	void testSaveAllEvent_writesTheDeltasAndEmptiesTheLog() {
		IInstanz created = _inse.createInstanz(ROOT, Type.SEND);

		_broker.send(EventConstants.SAVE_ALL, "save");

		assertThat(ProductRuntime.instanzFileExists(created.getOwnKey()), is(true));
		assertThat(_delta.getDeltas(), contains(IDeltaService.START_EVENT));
	}

	/**
	 * The log is the difference against what is on disk, so what a save wrote must
	 * not be offered for writing again.
	 */
	@Test
	void testSaveDeltas_twiceInARowWritesTheSecondTimeNothing() {
		IInstanz created = _inse.createInstanz(ROOT, Type.SEND);
		_delta.saveDeltas();

		_delta.saveDeltas();

		assertThat(ProductRuntime.instanzFileExists(created.getOwnKey()), is(true));
		assertThat(_delta.getDeltas(), contains(IDeltaService.START_EVENT));
	}

	/**
	 * A value deleted before the save was ever reached leaves the log holding both
	 * its new and its delete event. The delete has to win, or a file nobody
	 * references is left behind.
	 */
	@Test
	void testSaveDeltas_createAndDeleteWithinOneLog_leavesNoFile() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		SingleStringValue shortLived = _svs.createNew(SingleStringValue.class, owner.getOwnKey(), "parameter",
				"content", Type.SEND);
		_svs.markSingleValueAsDelete(shortLived.getOwnKey(), Type.SEND);

		_delta.saveDeltas();

		assertThat(ProductRuntime.valueFileExists(SingleValueType.SINGLE_STRING, shortLived.getOwnKey()), is(false));
		assertThat(ProductRuntime.reloadInstanz(owner.getOwnKey()).getSingleValues(SingleValueType.SINGLE_STRING)
				.keySet(), is(empty()));
	}
}
