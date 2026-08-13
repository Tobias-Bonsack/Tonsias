package de.tonsias.basis.osgi.test.system;

import static de.tonsias.basis.osgi.test.ProductRuntime.ROOT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.test.ProductRuntime;

/**
 * What a save does when one of its four steps cannot be carried out.
 * <p>
 * The log is the only record of what still needs writing, so a save that gives
 * up halfway and leaves its log standing does not just lose this one write: the
 * next save folds the same set again, fails on the same step again, and from
 * then on the application saves nothing at all without ever saying so. Two
 * things keep that from happening - a delete that finds no file has reached its
 * goal and is no failure, and whatever else goes wrong, the log is handed back
 * empty.
 * </p>
 *
 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/53">#53</a>
 */
public class DeltaSaveFailureSystemTest {

	/** no instanz is ever written under this key, it only names a path */
	private static final String NEVER_WRITTEN = "zzznotwritten";

	IInstanzService _inse;

	ISingleValueService _svs;

	IDeltaService _delta;

	Path _blocked;

	@BeforeEach
	void beforeEach() {
		ProductRuntime.start();
		_inse = ProductRuntime.instanzService();
		_svs = ProductRuntime.singleValueService();
		_delta = ProductRuntime.deltaService();

		ProductRuntime.flushDeltas();
	}

	@AfterEach
	void afterEach() {
		if (_blocked != null) {
			ProductRuntime.unblock(_blocked);
			_blocked = null;
		}
		ProductRuntime.flushDeltas();
	}

	/** a key whose instanz file cannot be deleted, however often it is tried */
	private String blockedInstanzKey() {
		_blocked = ProductRuntime.instanzFile(NEVER_WRITTEN);
		ProductRuntime.block(_blocked);
		return NEVER_WRITTEN;
	}

	// ---------- a delete with nothing to delete ----------

	@Test
	void testDeleteAll_instanzWithoutAFile_isNoFailure() {
		assertThat(_inse.deleteAll(Set.of(NEVER_WRITTEN)), is(true));
	}

	@Test
	void testDeleteAll_singleValueWithoutAFileInAnyFolder_isNoFailure() {
		assertThat(_svs.deleteAll(Set.of(NEVER_WRITTEN)), is(true));
	}

	/**
	 * The everyday way into it: something is marked for deletion that never made it
	 * to disk. Before, that alone was enough to wedge every following save.
	 */
	@Test
	void testSaveDeltas_deletingAnInstanzThatWasNeverWritten_savesTheRestAndEmptiesTheLog() {
		IInstanz kept = _inse.createInstanz(ROOT, Type.SEND);
		_inse.markInstanzAsDelete(NEVER_WRITTEN, Type.SEND);

		_delta.saveDeltas();

		assertThat(ProductRuntime.instanzFileExists(kept.getOwnKey()), is(true));
		assertThat(_delta.getDeltas(), contains(IDeltaService.START_EVENT));
	}

	/**
	 * The same value deleted twice over two saves - the second delete finds the
	 * file already gone, which is exactly what it wanted.
	 */
	@Test
	void testSaveDeltas_deletingASingleValueTwice_isNoFailure() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		SingleStringValue value = _svs.createNew(SingleStringValue.class, owner.getOwnKey(), "parameter", "content",
				Type.SEND);
		_delta.saveDeltas();
		_svs.markValueAsDelete(value.getOwnKey(), Type.SEND);
		_delta.saveDeltas();
		assertThat(ProductRuntime.valueFileExists(SingleValueType.SINGLE_STRING, value.getOwnKey()), is(false));

		_svs.markValueAsDelete(value.getOwnKey(), Type.SEND);
		_delta.saveDeltas();

		assertThat(_delta.getDeltas(), contains(IDeltaService.START_EVENT));
	}

	// ---------- a delete that really fails ----------

	/**
	 * The single values are written after the instanzes are deleted. A failing
	 * delete used to end the save right there, so a value change made in the same
	 * operation was silently dropped.
	 */
	@Test
	void testSaveDeltas_afterAFailingDeleteTheRemainingStepsStillRun() {
		String blocked = blockedInstanzKey();
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		SingleStringValue value = _svs.createNew(SingleStringValue.class, owner.getOwnKey(), "parameter", "content",
				Type.SEND);
		_inse.markInstanzAsDelete(blocked, Type.SEND);

		assertThrows(CompletionException.class, () -> _delta.saveDeltas());

		assertThat(ProductRuntime.instanzFileExists(owner.getOwnKey()), is(true));
		assertThat(ProductRuntime.valueFileExists(SingleValueType.SINGLE_STRING, value.getOwnKey()), is(true));
	}

	/** What went wrong is not swallowed - it is carried out as suppressed. */
	@Test
	void testSaveDeltas_aFailingDeleteIsReportedToTheCaller() {
		String blocked = blockedInstanzKey();
		_inse.markInstanzAsDelete(blocked, Type.SEND);

		CompletionException thrown = assertThrows(CompletionException.class, () -> _delta.saveDeltas());

		assertThat(thrown.getSuppressed(), is(arrayWithSize(1)));
	}

	/**
	 * And the point of the whole exercise: the failure does not outlive the save it
	 * happened in. The log is spent, so the next save is about what has happened
	 * since - not about the same broken delete all over again.
	 */
	@Test
	void testSaveDeltas_aFailingDeleteStillEmptiesTheLog() {
		String blocked = blockedInstanzKey();
		_inse.markInstanzAsDelete(blocked, Type.SEND);
		assertThrows(CompletionException.class, () -> _delta.saveDeltas());

		assertThat(_delta.getDeltas(), contains(IDeltaService.START_EVENT));

		IInstanz afterwards = _inse.createInstanz(ROOT, Type.SEND);
		_delta.saveDeltas();

		assertThat(ProductRuntime.instanzFileExists(afterwards.getOwnKey()), is(true));
		assertThat(_delta.getDeltas(), contains(IDeltaService.START_EVENT));
	}
}
