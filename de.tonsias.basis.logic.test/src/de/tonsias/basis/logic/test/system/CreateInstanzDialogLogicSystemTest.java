package de.tonsias.basis.logic.test.system;

import static de.tonsias.basis.osgi.test.ProductRuntime.ROOT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.service.prefs.BackingStoreException;

import de.tonsias.basis.logic.dialog.CreateInstanzDialogLogic;
import de.tonsias.basis.logic.dialog.CreateInstanzDialogLogic.TableRecord;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleInstanzValue;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService.Key;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants;
import de.tonsias.basis.osgi.test.EventRecorder;
import de.tonsias.basis.osgi.test.ProductRuntime;

/**
 * The logic behind the "create instanz" dialog, on the registered services.
 * <p>
 * What the dialog collects is a table of attributes; what it produces is one
 * new instanz carrying them, created inside a single operation bracket so the
 * Delta view shows it as one step. The whole hand-off runs in a {@link
 * org.eclipse.core.runtime.jobs.Job}, so the tests wait for the closing bracket
 * before looking at the model.
 * </p>
 */
public class CreateInstanzDialogLogicSystemTest {

	IInstanzService _inse;

	ISingleValueService _svs;

	IBasicPreferenceService _prefs;

	IEventBrokerBridge _broker;

	EventRecorder _recorder;

	String _configuredName;

	@BeforeEach
	void beforeEach() {
		ProductRuntime.start();
		_inse = ProductRuntime.instanzService();
		_svs = ProductRuntime.singleValueService();
		_prefs = ProductRuntime.preferenceService();
		_broker = ProductRuntime.broker();

		_configuredName = _prefs.getValue(Key.MODEL_VIEW_TEXT.getKey(), String.class).orElseThrow();
		_recorder = EventRecorder.subscribeToAllDeltasAndOperations(_broker);
	}

	@AfterEach
	void afterEach() throws BackingStoreException {
		_prefs.saveAsToString(Key.MODEL_VIEW_TEXT.getKey(), _configuredName);
		_recorder.unsubscribe();
		ProductRuntime.flushDeltas();
	}

	private CreateInstanzDialogLogic newLogic() {
		return new CreateInstanzDialogLogic(_inse, _svs, ProductRuntime.multiValueService(), _prefs);
	}

	// ---------- the table the dialog starts with ----------

	/**
	 * The dialog opens with one row already filled in, named after the preference
	 * the Model view labels its nodes with - so the usual case is one keystroke.
	 * <p>
	 * The shipped preference service seeds every one of its keys on first access,
	 * so there is no reachable state in which that preference is absent; the
	 * logic's fallback to an empty table stays dead code as long as that holds.
	 * </p>
	 */
	@Test
	void testNewLogic_seedsARowNamedAfterTheModelViewPreference() {
		var logic = newLogic();

		assertThat(logic.getInput(), hasSize(1));
		TableRecord row = logic.getInput().iterator().next();
		assertThat(row.type, is(SingleValueType.SINGLE_STRING));
		assertThat(row.parameterName, is(_configuredName));
	}

	@Test
	void testNewLogic_followsTheStoredPreferenceWhenItChanges() throws BackingStoreException {
		_prefs.saveAsToString(Key.MODEL_VIEW_TEXT.getKey(), "Bezeichnung");

		assertThat(newLogic().getInput().iterator().next().parameterName, is("Bezeichnung"));
	}

	@Test
	void testAddNewEntry_appendsADefaultRow() {
		var logic = newLogic();

		logic.addNewEntry();

		assertThat(logic.getInput(), hasSize(2));
		assertThat(logic.getInput().stream().map(row -> row.parameterName).toList(),
				contains(_configuredName, "parameterName"));
	}

	@Test
	void testRemoveSelectedEntry_dropsTheGivenRow() {
		var logic = newLogic();
		logic.addNewEntry();
		TableRecord toRemove = logic.getInput().iterator().next();

		logic.removeSelectedEntry(toRemove);

		assertThat(logic.getInput(), hasSize(1));
		assertThat(logic.getInput(), not(hasItem(toRemove)));
	}

	@Test
	void testRemoveSelectedEntry_anythingThatIsNotARowChangesNothing() {
		var logic = newLogic();

		logic.removeSelectedEntry("not a row");

		assertThat(logic.getInput(), hasSize(1));
	}

	/** The table viewer holds on to this collection, so it has to stay live. */
	@Test
	void testGetInput_isTheLiveTableAndReflectsEveryChange() {
		var logic = newLogic();
		var input = logic.getInput();

		logic.addNewEntry();

		assertThat(input, hasSize(2));
	}

	// ---------- a row that is a relation ----------

	/**
	 * The value column is the same column for every type, and its editor is not:
	 * for a relation it opens the tree the value dialog offers, so what the row
	 * holds is a key rather than text somebody typed.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/75">#75</a>
	 */
	@Test
	void testSetType_switchingToARelationDropsTheTypedText() {
		var logic = newLogic();
		TableRecord row = logic.getInput().iterator().next();
		row.value = "typed text";

		logic.setType(row, SingleValueType.SINGLE_INSTANZ);

		assertThat(row.type, is(SingleValueType.SINGLE_INSTANZ));
		assertThat(row.value, is(""));
	}

	/** and back the other way: an instanz key is no text anybody meant to write */
	@Test
	void testSetType_switchingAwayFromARelationDropsTheKey() {
		var logic = newLogic();
		TableRecord row = logic.getInput().iterator().next();
		logic.setType(row, SingleValueType.SINGLE_INSTANZ);
		row.value = _inse.createInstanz(ROOT, Type.SEND).getOwnKey();

		logic.setType(row, SingleValueType.SINGLE_STRING);

		assertThat(row.value, is(""));
	}

	/** a switch between two typed types leaves what is in the cell alone */
	@Test
	void testSetType_betweenTwoTypedTypesKeepsTheValue() {
		var logic = newLogic();
		TableRecord row = logic.getInput().iterator().next();
		row.value = "42";

		logic.setType(row, SingleValueType.SINGLE_INTEGER);

		assertThat(row.value, is("42"));
	}

	/**
	 * What the column shows. A relation stores a key, and a key is not what the
	 * user chose - the instanz is, and it reads here as it does in the tree.
	 */
	@Test
	void testValueLabel_aRelationReadsAsItsTarget() {
		var logic = newLogic();
		IInstanz target = _inse.createInstanz(ROOT, Type.SEND);
		TableRecord row = logic.getInput().iterator().next();
		logic.setType(row, SingleValueType.SINGLE_INSTANZ);
		row.value = target.getOwnKey();

		assertThat(logic.valueLabel(row), is(logic.instanzChoices().labelOf(target)));
	}

	@Test
	void testValueLabel_aRelationPointingNowhereShowsNothing() {
		var logic = newLogic();
		TableRecord row = logic.getInput().iterator().next();
		logic.setType(row, SingleValueType.SINGLE_INSTANZ);

		assertThat("nothing chosen yet", logic.valueLabel(row), is(""));

		row.value = "no-such-key";
		assertThat("a target that is gone", logic.valueLabel(row), is(""));
	}

	@Test
	void testValueLabel_everyOtherTypeReadsAsWhatIsInTheCell() {
		var logic = newLogic();
		TableRecord row = logic.getInput().iterator().next();
		row.value = "content";

		assertThat(logic.valueLabel(row), is("content"));
	}

	/**
	 * The row travels into {@code createNew} unchanged, so a relation created here
	 * has to end up pointing where the tree said - and be followable afterwards.
	 */
	@Test
	void testOkPressed_createsARelationPointingAtTheChosenInstanz() {
		var logic = newLogic();
		IInstanz parent = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz target = _inse.createInstanz(ROOT, Type.SEND);
		logic.setInstanzParent(parent);
		TableRecord row = logic.getInput().iterator().next();
		row.parameterName = "points at";
		logic.setType(row, SingleValueType.SINGLE_INSTANZ);
		row.value = target.getOwnKey();
		_recorder.clear();

		logic.okPressed(_broker);
		_recorder.awaitTopic(EventConstants.CLOSE_OPERATION);

		IInstanz created = _inse.resolveKey(parent.getChildren().iterator().next()).orElseThrow();
		String valueKey = created.getValues(SingleValueType.SINGLE_INSTANZ).keySet().iterator().next();
		SingleInstanzValue relation = _svs
				.resolveKey(SingleValueType.SINGLE_INSTANZ.getPath(), valueKey, SingleInstanzValue.class).orElseThrow();

		assertThat(relation.getValue(), is(target.getOwnKey()));
		assertThat(_inse.resolveInstanzValue(relation), is(java.util.Optional.of(target)));
		assertThat("and the target knows about it", target.getReferencingValueKeys(), hasItem(valueKey));
	}

	// ---------- creating the instanz ----------

	@Test
	void testOkPressed_createsTheInstanzUnderItsParentWithEveryRowAsAnAttribute() {
		var logic = newLogic();
		IInstanz parent = _inse.createInstanz(ROOT, Type.SEND);
		logic.setInstanzParent(parent);
		TableRecord row = logic.getInput().iterator().next();
		row.parameterName = "param";
		row.value = "value";
		_recorder.clear();

		logic.okPressed(_broker);
		_recorder.awaitTopic(EventConstants.CLOSE_OPERATION);

		assertThat(parent.getChildren(), hasSize(1));
		IInstanz created = _inse.resolveKey(parent.getChildren().iterator().next()).orElseThrow();
		assertThat(created.getValues(SingleValueType.SINGLE_STRING).keySet(), hasSize(1));

		String valueKey = created.getValues(SingleValueType.SINGLE_STRING).keySet().iterator().next();
		assertThat(created.getValues(SingleValueType.SINGLE_STRING).get(valueKey), is("param"));
		SingleStringValue value = _svs
				.resolveKey(SingleValueType.SINGLE_STRING.getPath(), valueKey, SingleStringValue.class).orElseThrow();
		assertThat(value.getValue(), is("value"));
		assertThat(value.getConnectedInstanzKeys(), contains(created.getOwnKey()));
	}

	/**
	 * Everything the dialog does belongs together, so it has to arrive between one
	 * pair of brackets - that is what the Delta view groups on.
	 */
	@Test
	void testOkPressed_wrapsTheWholeCreationInOneOperation() {
		var logic = newLogic();
		logic.setInstanzParent(_inse.createInstanz(ROOT, Type.SEND));
		TableRecord row = logic.getInput().iterator().next();
		row.parameterName = "param";
		row.value = "value";
		_recorder.clear();

		logic.okPressed(_broker);
		_recorder.awaitTopic(EventConstants.CLOSE_OPERATION);

		List<String> topics = _recorder.topics();
		assertThat(topics.get(0), is(EventConstants.OPEN_OPERATION));
		assertThat(topics.get(topics.size() - 1), is(EventConstants.CLOSE_OPERATION));
		assertThat(topics, hasItem(InstanzEventConstants.NEW));
		assertThat(topics, hasItem(SingleValueEventConstants.NEW));
	}

	@Test
	void testOkPressed_withoutAnyRowStillCreatesTheInstanzInsideTheBrackets() {
		var logic = newLogic();
		IInstanz parent = _inse.createInstanz(ROOT, Type.SEND);
		logic.setInstanzParent(parent);
		logic.removeSelectedEntry(logic.getInput().iterator().next());
		_recorder.clear();

		logic.okPressed(_broker);
		_recorder.awaitTopic(EventConstants.CLOSE_OPERATION);

		assertThat(parent.getChildren(), hasSize(1));
		IInstanz created = _inse.resolveKey(parent.getChildren().iterator().next()).orElseThrow();
		assertThat(created.getValues(SingleValueType.SINGLE_STRING).keySet(), hasSize(0));

		List<String> topics = _recorder.topics();
		assertThat(topics, hasSize(4));
		assertThat(topics.get(0), is(EventConstants.OPEN_OPERATION));
		assertThat(topics.get(3), is(EventConstants.CLOSE_OPERATION));
		// which of the two is served first is up to the event admin
		assertThat(topics.subList(1, 3),
				containsInAnyOrder(InstanzEventConstants.NEW, InstanzEventConstants.CHILD_LIST_CHANGE));
	}
}
