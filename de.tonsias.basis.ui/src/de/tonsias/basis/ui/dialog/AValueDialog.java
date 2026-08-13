package de.tonsias.basis.ui.dialog;

import java.util.Optional;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.ISingleValue;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.util.OsgiUtil;
import de.tonsias.basis.ui.i18n.Messages;

/**
 * The one-value half of {@link AValueDialogBase}: a widget holding one content,
 * and the two service calls that write it back.
 *
 * @param <T> the edited value
 * @param <C> the SWT widget this value is entered with - the subclass names it,
 *            so it works on its own widget without casting
 */
public abstract class AValueDialog<T extends ISingleValue<?>, C extends Control> extends AValueDialogBase<T, C> {

	ISingleValueService _sVService = OsgiUtil.getService(ISingleValueService.class);

	protected AValueDialog(Shell parentShell, T singleValue, IInstanz parent, Messages messages) {
		super(parentShell, singleValue, parent, messages);
	}

	@Override
	abstract SingleValueType getType();

	@Override
	protected String valuePrefill() {
		return _value.map(v -> v.getValue().toString()).orElse("");
	}

	/**
	 * What stands in the value control, in the shape {@code tryToSetValue} takes it:
	 * the text for the ones typed into, the check state for the boolean one.
	 */
	protected abstract Object getEnteredValue();

	/**
	 * Creates the value, or writes back both halves of the one being edited. The
	 * else branch used to pass on the name alone, so a changed number in the field
	 * was read for the OK button and then dropped. Both services answer
	 * {@code false} and fire nothing when nothing changed, so a pure rename stays
	 * one.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/72">#72</a>
	 */
	@Override
	protected void persist() {
		if (_value.isEmpty()) {
			_value = Optional.of(//
					_sVService.createNew(getValueClass(), //
							_instanz.getOwnKey(), //
							_nameText.getText(), //
							getEnteredValue(), //
							IEventBrokerBridge.Type.POST//
					));
		} else {
			String ownKey = _value.get().getOwnKey();
			_iService.changeValueName(_instanz.getOwnKey(), _type, ownKey, _nameText.getText(),
					IEventBrokerBridge.Type.POST);
			_sVService.changeValue(ownKey, getEnteredValue(), IEventBrokerBridge.Type.POST);
		}
	}

	public T getSingleValue() {
		return getCreatedValue();
	}
}
