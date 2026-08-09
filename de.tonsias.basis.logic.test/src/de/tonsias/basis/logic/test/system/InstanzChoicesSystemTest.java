package de.tonsias.basis.logic.test.system;

import static de.tonsias.basis.osgi.test.ProductRuntime.ROOT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tonsias.basis.logic.part.InstanzChoices;
import de.tonsias.basis.logic.part.InstanzChoices.Choice;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.test.ProductRuntime;

/**
 * The instanzen a relation can be pointed at, and how they read.
 * <p>
 * The runtime is shared by the whole bundle, so the tree already holds whatever
 * the other tests left below the root. Every assertion here is therefore about
 * the subtree this test built - what else is in the model is none of its
 * business.
 * </p>
 * <p>
 * The shape is a tree, not a flat list: the widget filters it and draws it as
 * one, which is what makes a large model selectable at all, see
 * <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/76">#76</a>.
 * </p>
 */
public class InstanzChoicesSystemTest {

	private static final String PARAMETER = "InstanzChoicesSystemTest label";

	IInstanzService _inse;

	ISingleValueService _svs;

	IBasicPreferenceService _prefs;

	InstanzChoices _choices;

	/** what the preference held before, so the shared runtime is left as found */
	Optional<String> _previousModelViewText;

	@BeforeEach
	void beforeEach() {
		ProductRuntime.start();
		_inse = ProductRuntime.instanzService();
		_svs = ProductRuntime.singleValueService();
		_prefs = ProductRuntime.preferenceService();
		_choices = new InstanzChoices(_inse, _svs, _prefs);

		_previousModelViewText = _prefs.getValue(IBasicPreferenceService.Key.MODEL_VIEW_TEXT.getKey(), String.class);
	}

	@AfterEach
	void afterEach() throws Exception {
		_prefs.saveAsToString(IBasicPreferenceService.Key.MODEL_VIEW_TEXT.getKey(),
				_previousModelViewText.orElse(""));
		ProductRuntime.flushDeltas();
	}

	private List<String> flatKeys() {
		return _choices.tree().flatten().stream().map(Choice::_key).toList();
	}

	@Test
	void testTree_startsAtTheRoot() {
		assertThat(_choices.tree()._key(), is(ROOT));
	}

	/** a whole branch, so the walk has to go past the first level */
	@Test
	void testTree_containsTheWholeSubtree() {
		IInstanz child = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz grandChild = _inse.createInstanz(child.getOwnKey(), Type.SEND);

		assertThat(flatKeys(), hasItem(child.getOwnKey()));
		assertThat(flatKeys(), hasItem(grandChild.getOwnKey()));
	}

	/**
	 * The nesting itself, which is the whole reason this is a tree: a child hangs
	 * below its parent rather than next to it.
	 */
	@Test
	void testTree_hangsEveryChildBelowItsParent() {
		IInstanz child = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz grandChild = _inse.createInstanz(child.getOwnKey(), Type.SEND);

		Choice childChoice = _choices.tree().find(child.getOwnKey()).orElseThrow();

		assertThat(childChoice._children().stream().map(Choice::_key).toList(), contains(grandChild.getOwnKey()));
		assertThat("the root holds it, not the grandchild's grandparent",
				_choices.tree()._children().stream().map(Choice::_key).toList(), hasItem(child.getOwnKey()));
	}

	/** depth first, the way the Model View draws it: a child before its sibling */
	@Test
	void testFlatten_isInTreeOrder() {
		IInstanz child = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz grandChild = _inse.createInstanz(child.getOwnKey(), Type.SEND);

		List<String> keys = flatKeys();

		assertThat(keys.indexOf(grandChild.getOwnKey()), is(keys.indexOf(child.getOwnKey()) + 1));
	}

	/** every instanz is offered once, however often the walk meets it */
	@Test
	void testFlatten_holdsNoKeyTwice() {
		_inse.createInstanz(ROOT, Type.SEND);

		List<String> keys = flatKeys();

		assertThat(keys, is(keys.stream().distinct().toList()));
	}

	// ---------- finding one again ----------

	/**
	 * What a stored relation is shown with: it holds a key, and the tree is the one
	 * place that says how that key reads.
	 */
	@Test
	void testFind_locatesAnInstanzAnywhereInTheTree() {
		IInstanz child = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz grandChild = _inse.createInstanz(child.getOwnKey(), Type.SEND);

		assertThat(_choices.tree().find(grandChild.getOwnKey()).orElseThrow()._key(), is(grandChild.getOwnKey()));
		assertThat(_choices.labelOf(grandChild.getOwnKey()), is(Optional.of(_choices.labelOf(grandChild))));
	}

	/**
	 * A relation pointing nowhere holds the empty string, and one whose target was
	 * deleted holds a key the tree no longer has. Neither is an error - both simply
	 * name no instanz.
	 */
	@Test
	void testFind_aKeyTheTreeDoesNotHoldIsEmpty() {
		assertThat(_choices.tree().find(""), is(Optional.empty()));
		assertThat(_choices.tree().find("no-such-key"), is(Optional.empty()));
		assertThat(_choices.labelOf("no-such-key"), is(Optional.empty()));
	}

	// ---------- how an instanz reads ----------

	/**
	 * The preference names a parameter; the instanz carrying a string value under
	 * that name reads as its content. This is the rule the Model View labels its
	 * nodes by, and the chooser of a relation has to agree with it.
	 */
	@Test
	void testLabelOf_takesTheValueThePreferenceNames() throws Exception {
		_prefs.saveAsToString(IBasicPreferenceService.Key.MODEL_VIEW_TEXT.getKey(), PARAMETER);
		IInstanz named = _inse.createInstanz(ROOT, Type.SEND);
		_svs.createNew(SingleStringValue.class, named.getOwnKey(), PARAMETER, "Kundennummer", Type.SEND);

		assertThat(_choices.labelOf(named), is("Kundennummer"));
		assertThat(_choices.tree().find(named.getOwnKey()).orElseThrow()._label(), is("Kundennummer"));
	}

	/** an instanz without that parameter falls back on what it says about itself */
	@Test
	void testLabelOf_fallsBackToToString() throws Exception {
		_prefs.saveAsToString(IBasicPreferenceService.Key.MODEL_VIEW_TEXT.getKey(), PARAMETER);
		IInstanz plain = _inse.createInstanz(ROOT, Type.SEND);

		assertThat(_choices.labelOf(plain), is(plain.toString()));
	}

	/** and so does every instanz while the preference names nothing at all */
	@Test
	void testLabelOf_fallsBackWhenThePreferenceIsUnset() throws Exception {
		_prefs.saveAsToString(IBasicPreferenceService.Key.MODEL_VIEW_TEXT.getKey(), "");
		IInstanz named = _inse.createInstanz(ROOT, Type.SEND);
		_svs.createNew(SingleStringValue.class, named.getOwnKey(), PARAMETER, "Kundennummer", Type.SEND);

		assertThat(_choices.labelOf(named), is(named.toString()));
	}

	/** the label a choice carries is the one labelOf gives - one rule, not two */
	@Test
	void testTree_labelsEveryEntryTheWayLabelOfDoes() {
		IInstanz child = _inse.createInstanz(ROOT, Type.SEND);

		Choice choice = _choices.tree().find(child.getOwnKey()).orElseThrow();

		assertThat(choice._label(), is(_choices.labelOf(child)));
	}

	/**
	 * Nothing is filtered out: an instanz may point at itself. A relation is no
	 * parent-child edge, so it cannot build a cycle in the tree, and there is no
	 * reason to keep the user from expressing one.
	 */
	@Test
	void testTree_containsTheInstanzItself() {
		IInstanz self = _inse.createInstanz(ROOT, Type.SEND);

		assertThat(flatKeys(), hasItem(self.getOwnKey()));
	}
}
