package de.tonsias.basis.osgi.test.system;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.service.event.Event;

import de.tonsias.basis.data.access.osgi.intf.LoadService;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.Instanz;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants;
import de.tonsias.basis.osgi.test.E4ServiceContext;
import de.tonsias.basis.osgi.util.OsgiUtil;

/**
 * The other half of the event system: what the chains of
 * {@link EventChainSystemTest} end up doing to the files.
 * <p>
 * Everything here goes through the real {@code DeltaServiceImpl}, which
 * subscribes to the same two wildcard topics the services fire on, folds its
 * event log into four key sets and hands them to the services. So a chain that
 * loses an event loses a file, and one that fires an event too many deletes a
 * file that is still referenced. The assertions therefore never look at the
 * caches: every object is read back from disk with the {@link LoadService}.
 * </p>
 */
public class DeltaPersistenceSystemTest {

	private static final String ROOT = "0";

	private static final String INSTANZ_PATH = "instanz/";

	IInstanzService _inse;

	ISingleValueService _svs;

	IDeltaService _delta;

	IEventBrokerBridge _broker;

	LoadService _load;

	@BeforeEach
	void beforeEach() {
		E4ServiceContext.prime();
		_inse = OsgiUtil.getService(IInstanzService.class);
		_svs = OsgiUtil.getService(ISingleValueService.class);
		_delta = OsgiUtil.getService(IDeltaService.class);
		_broker = OsgiUtil.getService(IEventBrokerBridge.class);
		_load = OsgiUtil.getService(LoadService.class);
		// in the product ModelView creates the root at start-up
		_inse.getRoot();

		flush();
	}

	@Test
	void testCreateInstanz_savesTheNewInstanzAndItsParent() {
		IInstanz child = _inse.createInstanz(ROOT, Type.SEND);
		// keys are lower case only, so a brand new key can not collide with an already
		// written file on a case insensitive file system (issue #35)
		assertThat(Files.exists(instanzFile(child.getOwnKey())), is(false));

		_delta.saveDeltas();

		assertThat(Files.exists(instanzFile(child.getOwnKey())), is(true));
		assertThat(reloadInstanz(child.getOwnKey()).getParentKey(), is(ROOT));
		// the parent is only touched by the propagated child list change - without it
		// the tree would be broken on the next start
		assertThat(reloadInstanz(ROOT).getChildren(), hasItem(child.getOwnKey()));
	}

	@Test
	void testSaveAllEvent_triggersTheSave() {
		IInstanz child = _inse.createInstanz(ROOT, Type.SEND);

		_broker.send(EventConstants.SAVE_ALL, "save");

		assertThat(Files.exists(instanzFile(child.getOwnKey())), is(true));
		assertThat(_delta.getDeltas(), contains(IDeltaService.START_EVENT));
	}

	/**
	 * Everything the services put on the bus has to reach the log, because the log
	 * is the only record of what still needs saving.
	 */
	@Test
	void testDeltaLog_collectsEveryEventOfTheChain() {
		int before = _delta.getDeltas().size();

		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		_svs.createNew(SingleStringValue.class, owner.getOwnKey(), "parameter", "content", Type.SEND);

		assertThat(deltaTopicsSince(before),
				containsInAnyOrder(InstanzEventConstants.NEW, InstanzEventConstants.CHILD_LIST_CHANGE,
						SingleValueEventConstants.NEW, InstanzEventConstants.VALUE_LIST_CHANGE));

		_delta.saveDeltas();

		assertThat(_delta.getDeltas(), contains(IDeltaService.START_EVENT));
	}

	@Test
	void testCreateSingleValue_savesTheValueAndTheOwnerThatReferencesIt() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		SingleStringValue value = _svs.createNew(SingleStringValue.class, owner.getOwnKey(), "parameter", "content",
				Type.SEND);

		_delta.saveDeltas();

		assertThat(Files.exists(stringValueFile(value.getOwnKey())), is(true));
		SingleStringValue reloaded = reloadStringValue(value.getOwnKey());
		assertThat(reloaded.getValue(), is("content"));
		assertThat(reloaded.getConnectedInstanzKeys(), contains(owner.getOwnKey()));

		assertThat(reloadInstanz(owner.getOwnKey()).getValues(SingleValueType.SINGLE_STRING)
				.get(value.getOwnKey()), is("parameter"));
	}

	@Test
	void testChangeValue_isPersisted() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		SingleStringValue value = _svs.createNew(SingleStringValue.class, owner.getOwnKey(), "parameter", "old",
				Type.SEND);
		_delta.saveDeltas();

		_svs.changeValue(value.getOwnKey(), "new", Type.SEND);
		_delta.saveDeltas();

		assertThat(reloadStringValue(value.getOwnKey()).getValue(), is("new"));
	}

	@Test
	void testAddToParent_persistsTheLinkOnBothSides() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz secondOwner = _inse.createInstanz(ROOT, Type.SEND);
		SingleStringValue value = _svs.createNew(SingleStringValue.class, owner.getOwnKey(), "parameter", "content",
				Type.SEND);
		_delta.saveDeltas();

		_svs.addToParent(SingleValueType.SINGLE_STRING, value.getOwnKey(), secondOwner.getOwnKey(), Type.SEND);
		_delta.saveDeltas();

		assertThat(reloadStringValue(value.getOwnKey()).getConnectedInstanzKeys(),
				containsInAnyOrder(owner.getOwnKey(), secondOwner.getOwnKey()));
		assertThat(reloadInstanz(secondOwner.getOwnKey()).getValues(SingleValueType.SINGLE_STRING)
				.containsKey(value.getOwnKey()), is(true));
	}

	@Test
	void testChangeSingleValueName_isPersistedOnTheOwner() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		SingleStringValue value = _svs.createNew(SingleStringValue.class, owner.getOwnKey(), "oldName", "content",
				Type.SEND);
		_delta.saveDeltas();

		_inse.changeSingleValueName(owner.getOwnKey(), SingleValueType.SINGLE_STRING, value.getOwnKey(), "newName",
				Type.SEND);
		_delta.saveDeltas();

		assertThat(reloadInstanz(owner.getOwnKey()).getValues(SingleValueType.SINGLE_STRING)
				.get(value.getOwnKey()), is("newName"));
	}

	/**
	 * The delete event carries the owners with it, and the value list changes it
	 * causes are what puts those owners into the save set - so the file goes and
	 * no owner is left pointing at it.
	 */
	@Test
	void testMarkSingleValueAsDelete_deletesTheFileAndRewritesEveryOwner() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz secondOwner = _inse.createInstanz(ROOT, Type.SEND);
		SingleStringValue value = _svs.createNew(SingleStringValue.class, owner.getOwnKey(), "parameter", "content",
				Type.SEND);
		_svs.addToParent(SingleValueType.SINGLE_STRING, value.getOwnKey(), secondOwner.getOwnKey(), Type.SEND);
		_delta.saveDeltas();
		assertThat(Files.exists(stringValueFile(value.getOwnKey())), is(true));

		_svs.markSingleValueAsDelete(value.getOwnKey(), Type.SEND);
		_delta.saveDeltas();

		assertThat(Files.exists(stringValueFile(value.getOwnKey())), is(false));
		assertThat(reloadInstanz(owner.getOwnKey()).getValues(SingleValueType.SINGLE_STRING)
				.containsKey(value.getOwnKey()), is(false));
		assertThat(reloadInstanz(secondOwner.getOwnKey()).getValues(SingleValueType.SINGLE_STRING)
				.containsKey(value.getOwnKey()), is(false));
	}

	@Test
	void testRemoveChild_deletesTheWholeSubtreeFromDisk() {
		IInstanz branch = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz leaf = _inse.createInstanz(branch.getOwnKey(), Type.SEND);
		IInstanz deepLeaf = _inse.createInstanz(leaf.getOwnKey(), Type.SEND);
		_delta.saveDeltas();
		assertThat(Files.exists(instanzFile(deepLeaf.getOwnKey())), is(true));

		_inse.removeChild(ROOT, branch.getOwnKey(), Type.SEND);
		_delta.saveDeltas();

		assertThat(Files.exists(instanzFile(branch.getOwnKey())), is(false));
		assertThat(Files.exists(instanzFile(leaf.getOwnKey())), is(false));
		assertThat(Files.exists(instanzFile(deepLeaf.getOwnKey())), is(false));
		assertThat(reloadInstanz(ROOT).getChildren(), not(hasItem(branch.getOwnKey())));
	}

	/**
	 * A move runs through the same child list change as a delete does. If the
	 * listener ever stopped telling the two apart, this is where it would show:
	 * the moved instanz would be deleted instead of re-parented.
	 */
	@Test
	void testPutChild_move_rewritesThreeFilesAndDeletesNone() {
		IInstanz oldParent = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz newParent = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz moved = _inse.createInstanz(oldParent.getOwnKey(), Type.SEND);
		_delta.saveDeltas();

		_inse.putChild(newParent.getOwnKey(), moved.getOwnKey(), Type.SEND);
		_delta.saveDeltas();

		assertThat(Files.exists(instanzFile(moved.getOwnKey())), is(true));
		assertThat(reloadInstanz(moved.getOwnKey()).getParentKey(), is(newParent.getOwnKey()));
		assertThat(reloadInstanz(newParent.getOwnKey()).getChildren(), contains(moved.getOwnKey()));
		assertThat(reloadInstanz(oldParent.getOwnKey()).getChildren(), not(hasItem(moved.getOwnKey())));
	}

	@Test
	void testSaveDeltas_withoutAnyChange_writesNothingNew() {
		IInstanz untouched = _inse.createInstanz(ROOT, Type.SEND);
		_delta.saveDeltas();

		_delta.saveDeltas();

		assertThat(Files.exists(instanzFile(untouched.getOwnKey())), is(true));
		assertThat(_delta.getDeltas(), contains(IDeltaService.START_EVENT));
	}

	/**
	 * Create and delete within one bracket: the log holds both the new and the
	 * delete event for the same key, and the delete has to win - otherwise a file
	 * nobody references anymore is left behind.
	 */
	@Test
	void testSaveDeltas_createAndDeleteBeforeTheSave_leavesNoFile() {
		IInstanz shortLived = _inse.createInstanz(ROOT, Type.SEND);
		_inse.removeChild(ROOT, shortLived.getOwnKey(), Type.SEND);

		_delta.saveDeltas();

		assertThat(Files.exists(instanzFile(shortLived.getOwnKey())), is(false));
		assertThat(reloadInstanz(ROOT).getChildren(), not(hasItem(shortLived.getOwnKey())));
	}

	/**
	 * events the delta service has taken since the given position in its log
	 */
	private List<String> deltaTopicsSince(int index) {
		return _delta.getDeltas().stream().skip(index).map(Event::getTopic).toList();
	}

	private void flush() {
		_delta.saveDeltas();
	}

	private Instanz reloadInstanz(String key) {
		Instanz loaded = _load.loadFromGson(INSTANZ_PATH + key, Instanz.class);
		assertThat("no file for instanz " + key, loaded, is(not(nullValue())));
		return loaded;
	}

	private SingleStringValue reloadStringValue(String key) {
		SingleStringValue loaded = _load.loadFromGson(SingleValueType.SINGLE_STRING.getPath() + key,
				SingleStringValue.class);
		assertThat("no file for single value " + key, loaded, is(not(nullValue())));
		return loaded;
	}

	private static Path instanzFile(String key) {
		return workspace().resolve(INSTANZ_PATH + key + ".json");
	}

	private static Path stringValueFile(String key) {
		return workspace().resolve(SingleValueType.SINGLE_STRING.getPath() + key + ".json");
	}

	/**
	 * the directory the persistence services write to, resolved the same way
	 * {@code InstanceLocationUtil} does
	 */
	private static Path workspace() {
		URL url = Platform.getInstanceLocation().getURL();
		try {
			return Paths.get(URIUtil.toURI(url));
		} catch (URISyntaxException e) {
			throw new AssertionError("unusable instance location: " + url, e);
		}
	}
}
