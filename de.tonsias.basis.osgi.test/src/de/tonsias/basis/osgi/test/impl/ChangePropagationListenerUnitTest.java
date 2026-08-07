package de.tonsias.basis.osgi.test.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.event.Event;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.Instanz;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.ChangeType;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.InstanzEvent;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.LinkedChildChangeEvent;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.LinkedValueChangeEvent;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.ParentChange;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.LinkedInstanzChangeEvent;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.SingleValueDeleteEvent;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.SingleValueNewEvent;
import de.tonsias.basis.osgi.util.ChangePropagationListener;

/**
 * The listener is what keeps both ends of every relation in sync, and every one
 * of its handlers re-enters the services with {@link Type#SEND}. These tests
 * pin down which call each incoming event turns into.
 */
@ExtendWith(MockitoExtension.class)
public class ChangePropagationListenerUnitTest {

	@Mock
	IInstanzService _instanz;

	@Mock
	ISingleValueService _singleValue;

	@InjectMocks
	ChangePropagationListener _listener;

	private static Event event(String topic, Object data) {
		return new Event(topic, Map.of(IEventBroker.DATA, data));
	}

	// ---------- instanz to instanz ----------

	@Test
	void testNewInstanz_addsTheKeyToItsParent() {
		_listener.newInstanzListener(event(InstanzEventConstants.NEW, new InstanzEvent("child", "parent")));

		verify(_instanz).putChild("parent", "child", Type.SEND);
	}

	@Test
	void testChildAdded_setsTheParentOnEveryChild() {
		_listener.changeChildCollectionListener(event(InstanzEventConstants.CHILD_LIST_CHANGE,
				new LinkedChildChangeEvent("parent", ChangeType.ADD, List.of("c1", "c2"))));

		verify(_instanz).changeParent("c1", "parent", Type.SEND);
		verify(_instanz).changeParent("c2", "parent", Type.SEND);
	}

	@Test
	void testChildRemoved_detachesTheSubtree() {
		Instanz child = new Instanz("child");
		child.setParentKey("parent");
		when(_instanz.resolveKey("child")).thenReturn(Optional.of(child));

		_listener.changeChildCollectionListener(event(InstanzEventConstants.CHILD_LIST_CHANGE,
				new LinkedChildChangeEvent("parent", ChangeType.REMOVE, List.of("child"))));

		verify(_instanz).removeSubtreeInstanz("child", Type.SEND);
	}

	/**
	 * The child already belongs to somebody else, so this removal is the other half
	 * of a move and must not delete anything - that guard is what keeps the
	 * listener from looping back into the services.
	 */
	@Test
	void testChildRemoved_childOfAnotherParentIsLeftAlone() {
		Instanz child = new Instanz("child");
		child.setParentKey("newParent");
		when(_instanz.resolveKey("child")).thenReturn(Optional.of(child));

		_listener.changeChildCollectionListener(event(InstanzEventConstants.CHILD_LIST_CHANGE,
				new LinkedChildChangeEvent("oldParent", ChangeType.REMOVE, List.of("child"))));

		verify(_instanz, never()).removeSubtreeInstanz(anyString(), any());
	}

	@Test
	void testChildRemoved_unresolvableChildIsLeftAlone() {
		when(_instanz.resolveKey("child")).thenReturn(Optional.empty());

		_listener.changeChildCollectionListener(event(InstanzEventConstants.CHILD_LIST_CHANGE,
				new LinkedChildChangeEvent("parent", ChangeType.REMOVE, List.of("child"))));

		verify(_instanz, never()).removeSubtreeInstanz(anyString(), any());
	}

	@Test
	void testParentChanged_movesTheKeyBetweenBothChildLists() {
		_listener.changeParentListener(
				event(InstanzEventConstants.PARENT_CHANGE, new ParentChange("child", "newParent", "oldParent")));

		verify(_instanz).putChild("newParent", "child", Type.SEND);
		verify(_instanz).removeChild("oldParent", "child", Type.SEND);
	}

	@Test
	void testInstanzDeleted_marksEveryChild() {
		Instanz deleted = new Instanz("gone");
		deleted.addChildKeys("c1", "c2");
		when(_instanz.resolveKey("gone")).thenReturn(Optional.of(deleted));

		_listener.deleteInstanzListener(event(InstanzEventConstants.DELETE, new InstanzEvent("gone", null)));

		verify(_instanz).markInstanzAsDelete("c1", Type.SEND);
		verify(_instanz).markInstanzAsDelete("c2", Type.SEND);
	}

	@Test
	void testInstanzDeleted_unresolvableKeyIsANoOp() {
		when(_instanz.resolveKey("gone")).thenReturn(Optional.empty());

		_listener.deleteInstanzListener(event(InstanzEventConstants.DELETE, new InstanzEvent("gone", null)));

		verify(_instanz, never()).markInstanzAsDelete(anyString(), any());
	}

	// ---------- instanz to single value ----------

	@Test
	void testValueListAdded_linksTheValueBackToTheInstanz() {
		_listener.putSingleValueListener(event(InstanzEventConstants.VALUE_LIST_CHANGE, new LinkedValueChangeEvent(
				"instanz", SingleValueType.SINGLE_STRING, ChangeType.ADD, List.of("v1", "v2"))));

		verify(_singleValue).addToParent(SingleValueType.SINGLE_STRING, "v1", "instanz", Type.SEND);
		verify(_singleValue).addToParent(SingleValueType.SINGLE_STRING, "v2", "instanz", Type.SEND);
	}

	@Test
	void testValueListRemoved_isNotPropagatedYet() {
		_listener.putSingleValueListener(event(InstanzEventConstants.VALUE_LIST_CHANGE, new LinkedValueChangeEvent(
				"instanz", SingleValueType.SINGLE_STRING, ChangeType.REMOVE, List.of("v1"))));

		verifyNoInteractions(_singleValue);
	}

	// ---------- single value to instanz ----------

	@Test
	void testNewSingleValue_isPutOnEveryOwner() {
		_listener.newSingleValueListener(event(SingleValueEventConstants.NEW,
				new SingleValueNewEvent(SingleValueType.SINGLE_STRING, "vKey", "vName", List.of("i1", "i2"))));

		verify(_instanz).putSingleValue("i1", SingleValueType.SINGLE_STRING, "vKey", "vName", Type.SEND);
		verify(_instanz).putSingleValue("i2", SingleValueType.SINGLE_STRING, "vKey", "vName", Type.SEND);
	}

	@Test
	void testDeletedSingleValue_isUnlinkedFromEveryOwner() {
		_listener.removeSingleValueListener(event(SingleValueEventConstants.DELETE,
				new SingleValueDeleteEvent(SingleValueType.SINGLE_STRING, "vKey", List.of("i1", "i2"))));

		verify(_instanz).removeValueKey(List.of("i1", "i2"), SingleValueType.SINGLE_STRING, "vKey", Type.SEND);
	}

	@Test
	void testInstanzListAdded_putsTheValueOnTheNewOwner() {
		_listener.addToParentListener(event(SingleValueEventConstants.INSTANZ_LIST_CHANGE,
				new LinkedInstanzChangeEvent("vKey", SingleValueType.SINGLE_STRING,
						LinkedInstanzChangeEvent.ChangeType.ADD, List.of("i1"))));

		verify(_instanz).putSingleValue("i1", SingleValueType.SINGLE_STRING, "vKey", null, Type.SEND);
	}

	@Test
	void testInstanzListRemoved_isNotPropagatedYet() {
		_listener.addToParentListener(event(SingleValueEventConstants.INSTANZ_LIST_CHANGE,
				new LinkedInstanzChangeEvent("vKey", SingleValueType.SINGLE_STRING,
						LinkedInstanzChangeEvent.ChangeType.REMOVE, List.of("i1"))));

		verifyNoInteractions(_instanz);
	}

	@Test
	void testChildListChange_withoutAChangeTypeIsRejected() {
		Event broken = event(InstanzEventConstants.CHILD_LIST_CHANGE,
				new LinkedChildChangeEvent("parent", null, List.of("c1")));

		assertThrows(NullPointerException.class, () -> _listener.changeChildCollectionListener(broken));
		verify(_instanz, never()).changeParent(anyString(), eq("parent"), any());
	}
}
