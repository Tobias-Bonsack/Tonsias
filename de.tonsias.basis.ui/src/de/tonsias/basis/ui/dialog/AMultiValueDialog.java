package de.tonsias.basis.ui.dialog;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.logic.part.InstanzChoices;
import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.impl.value.ValueContentRules;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.IMultiValue;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IMultiValueService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.util.OsgiUtil;
import de.tonsias.basis.ui.i18n.Messages;
import de.tonsias.basis.ui.widget.MultiElementList;

/**
 * The list half of {@link AValueDialogBase}. Every content uses the same
 * {@link MultiElementList}, so the five subclasses only say which type they are -
 * there is no per-content widget to write, the way there is for a single value.
 *
 * @param <T> the edited value
 */
public abstract class AMultiValueDialog<T extends IMultiValue<?>> extends AValueDialogBase<T, MultiElementList> {

	IMultiValueService _mVService = OsgiUtil.getService(IMultiValueService.class);

	/** the list as OK left it, readable once the widget is gone */
	private List<Object> _enteredElements = List.of();

	private boolean _writeOnOk = true;

	protected AMultiValueDialog(Shell parentShell, T multiValue, IInstanz parent, Messages messages) {
		super(parentShell, multiValue, parent, messages);
	}

	/**
	 * Whether OK writes the list back itself. The Instanz View queues what its
	 * widgets did and applies it all on save, so there it must not - it takes
	 * {@link #getEnteredElements()} and makes a job of it.
	 */
	public void setWriteOnOk(boolean writeOnOk) {
		_writeOnOk = writeOnOk;
	}

	public List<Object> getEnteredElements() {
		return _enteredElements;
	}

	@Override
	abstract MultiValueType getType();

	@Override
	protected String valueSideLabel() {
		return _messages.dialog_value_elements;
	}

	@Override
	protected MultiElementList createValueControl(Composite composite, String valuePrefill) {
		MultiElementList list = new MultiElementList(composite, getType().getContentType(), _messages,
				this::instanzChoices);
		_value.ifPresent(value -> list.setElements(value.getValues()));
		list.onChanged(this::refreshOkButton);
		return list;
	}

	/**
	 * The instanzen a relation can be pointed at. Built per call rather than kept:
	 * the dialog stays open while the model can change under it.
	 */
	private InstanzChoices instanzChoices() {
		return new InstanzChoices(_iService, OsgiUtil.getService(ISingleValueService.class),
				OsgiUtil.getService(IBasicPreferenceService.class));
	}

	/**
	 * An empty list is a value, the way {@code ""} and {@code false} are: a list
	 * filled later is a thing somebody means to create. What may not get in is an
	 * element the type would not read, or one that is already there - and both
	 * questions are put to the type itself, so no rule is restated here.
	 * <p>
	 * The widget refuses both at the door, so this is the second lock rather than
	 * the first: it is what keeps a list that got in some other way from being
	 * written back.
	 * </p>
	 */
	@Override
	protected boolean isValueAcceptable() {
		List<Object> elements = _valueControl.getElements();
		boolean everyElementReadable = elements.stream()
				.allMatch(element -> ValueContentRules.convert(getType().getContentType(), element).isPresent());
		return everyElementReadable && elements.size() == Set.copyOf(elements).size();
	}

	/**
	 * Creates the value, or writes back both halves of the one being edited. The
	 * whole list goes over at once and the service turns it into what came and what
	 * went - both answer {@code false} and fire nothing when nothing changed, so a
	 * pure rename stays one.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/72">#72</a>
	 */
	@Override
	protected void persist() {
		_enteredElements = _valueControl.getElements();
		if (!_writeOnOk) {
			return;
		}

		if (_value.isEmpty()) {
			_value = Optional.of(//
					_mVService.createNew(getValueClass(), //
							_instanz.getOwnKey(), //
							_nameText.getText(), //
							_valueControl.getElements(), //
							IEventBrokerBridge.Type.POST//
					));
		} else {
			String ownKey = _value.get().getOwnKey();
			_iService.changeValueName(_instanz.getOwnKey(), _type, ownKey, _nameText.getText(),
					IEventBrokerBridge.Type.POST);
			_mVService.changeElements(ownKey, _valueControl.getElements(), IEventBrokerBridge.Type.POST);
		}
	}

	public T getMultiValue() {
		return getCreatedValue();
	}
}
