package de.tonsias.basis.ui.dialog;

import java.util.Optional;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.widgets.LabelFactory;
import org.eclipse.jface.widgets.TextFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.ISingleValue;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.IKeyService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.util.OsgiUtil;
import de.tonsias.basis.ui.i18n.Messages;

/**
 * @param <T> the edited value
 * @param <C> the SWT widget this value is entered with - the subclass names it,
 *            so it works on its own widget without casting
 */
public abstract class AValueDialog<T extends ISingleValue<?>, C extends Control> extends Dialog {

	IKeyService _keyService = OsgiUtil.getService(IKeyService.class);

	ISingleValueService _sVService = OsgiUtil.getService(ISingleValueService.class);

	IInstanzService _iService = OsgiUtil.getService(IInstanzService.class);

	// empty while a new value is being created, and set by okPressed once it is.
	// Present means an existing value is being edited - a path ModelView does not
	// open today, it only ever creates, so the tests are the only ones on it
	Optional<T> _value;

	IInstanz _instanz;

	Text _nameText;

	Label _nameMessage;

	C _valueControl;

	SingleValueType _type;

	private Messages _messages;

	protected AValueDialog(Shell parentShell, T singleValue, IInstanz parent, Messages messages) {
		super(parentShell);
		_messages = messages;
		_value = Optional.ofNullable(singleValue);
		_instanz = parent;
		_type = getType();
	}

	abstract SingleValueType getType();
	
	@Override
	protected Point getInitialLocation(Point initialSize) {
		Point point = new Point(400, 600);
		getShell().setMinimumSize(point);
		return point;
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		GridLayoutFactory.fillDefaults().numColumns(2).spacing(10, 10).extendedMargins(10, 10, 10, 10)
				.applyTo(composite);

		createInstanzPart(composite);

		Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(separator);

		createSingleValuePart(composite);

		return composite;
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Control buttonBar = super.createButtonBar(parent);
		getButton(IDialogConstants.CANCEL_ID).setText(_messages.constant_cancel);
		
		getButton(IDialogConstants.OK_ID).addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.CR && e.stateMask == SWT.CONTROL) {
					e.doit = false;
					okPressed();
				}
			}
		});

		// Dialog.createContents builds the dialog area before the button bar, so the
		// value control is there to be judged and the button, only now, to be set.
		refreshOkButton();

		return buttonBar;
	}

	/**
	 * Whether the value control currently holds something the type's
	 * {@code tryToSetValue} would take. The base answer is yes - a text field of any
	 * content is a string, and a check box cannot hold an invalid state; the numeric
	 * dialogs put the question to their type.
	 */
	protected boolean isValueAcceptable() {
		return true;
	}

	/**
	 * Whether the name field holds a name that is free on this instanz. Names are
	 * the inverse side of the {@code BiMap} in {@link IInstanz}, so a second value
	 * of the same type under the same name would push the first one out of it. The
	 * name a stored value already carries stays acceptable - it belongs to the value
	 * being edited, and the dialog opens with it in the field.
	 */
	protected boolean isNameAcceptable() {
		String owner = _instanz.getSingleValues(_type).inverse().get(_nameText.getText());
		return owner == null || owner.equals(_value.map(v -> v.getOwnKey()).orElse(null));
	}

	/**
	 * Puts the OK button into the state the dialog as a whole calls for: both
	 * {@link #isNameAcceptable()} and {@link #isValueAcceptable()} have to be
	 * content with what stands in the fields. Runs once the button bar exists and
	 * again from the name listener and from whatever listener the subclass puts on
	 * its value control, so an untouched field is judged by the same rule as a typed
	 * one, and neither of the two checks can hand the button back for input the
	 * other has just rejected.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/62">#62</a>
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/67">#67</a>
	 */
	protected void refreshOkButton() {
		getButton(IDialogConstants.OK_ID).setEnabled(isNameAcceptable() && isValueAcceptable());
	}

	private void createInstanzPart(Composite composite) {
		Label instanzLabel = LabelFactory.newLabel(SWT.None).text(_messages.dialog_value_instanzSide).create(composite);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(instanzLabel);

		Label keyLabel = LabelFactory.newLabel(SWT.None).text(_messages.constant_key).create(composite);
		GridDataFactory.fillDefaults().applyTo(keyLabel);

		String keyString = _value.map(v -> v.getOwnKey()).orElse(_keyService.previewNextKey());
		Text keyText = TextFactory.newText(SWT.None).text(keyString).enabled(false).create(composite);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(keyText);

		Label nameLabel = LabelFactory.newLabel(SWT.None).text(_messages.constant_name).create(composite);
		GridDataFactory.fillDefaults().applyTo(nameLabel);

		String name = _instanz.getSingleValues(_type).getOrDefault(keyString, "");
		_nameText = TextFactory.newText(SWT.SEARCH).text(name).enabled(true).create(composite);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_nameText);

		// the reason the OK button is off, on a widget that shows it while there is
		// text in the field. Text.setMessage used to carry it, which is the placeholder
		// SWT only draws over an empty field - so it was never on screen in the one
		// case it was set for. See https://github.com/Tobias-Bonsack/Tonsias/issues/71
		_nameMessage = LabelFactory.newLabel(SWT.None).text("").create(composite);
		_nameMessage.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(_nameMessage);

		// the listener marks the field and leaves the button to refreshOkButton, which
		// is the one place that knows about both checks
		_nameText.addModifyListener(modifyEvent -> {
			refreshNameMessage();
			refreshOkButton();
		});
		refreshNameMessage();
	}

	/**
	 * Says why the name is refused, and marks the field, for whatever stands in it -
	 * called for the name a dialog opens with as well, so an untouched field is
	 * judged like a typed one. Leaves the OK button alone: that one is
	 * {@link #refreshOkButton()}'s, which knows about the value too.
	 */
	private void refreshNameMessage() {
		boolean acceptable = isNameAcceptable();
		_nameText.setBackground(acceptable ? null : Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		_nameMessage.setText(acceptable ? "" : _messages.dialog_value_usedName);
	}

	private void createSingleValuePart(Composite composite) {
		Label singleValueLabel = LabelFactory.newLabel(SWT.None).text(_messages.dialog_value_valueSide)
				.create(composite);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(singleValueLabel);

		Label valueLabel = LabelFactory.newLabel(SWT.None).text(_messages.constant_singleValue).create(composite);
		GridDataFactory.fillDefaults().applyTo(valueLabel);

		String valueString = _value.map(v -> v.getValue().toString()).orElse("");
		_valueControl = createValueControl(composite, valueString);
	}

	/**
	 * Creates the widget the value is entered with. The subclass decides its type
	 * and prefills it from {@code valueString}, which is empty for a new value.
	 */
	protected abstract C createValueControl(Composite composite, String valueString);

	/**
	 * What stands in the value control, in the shape {@code tryToSetValue} takes it:
	 * the text for the ones typed into, the check state for the boolean one.
	 */
	protected abstract Object getEnteredValue();

	/**
	 * The type this dialog edits, as the class {@code createNew} is handed. It is
	 * the one in {@link #getType()}, typed, so what comes back fits {@code _value}.
	 */
	protected abstract Class<T> getValueClass();

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
	protected void okPressed() {
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
			_iService.changeSingleValueName(_instanz.getOwnKey(), _type, ownKey, _nameText.getText(),
					IEventBrokerBridge.Type.POST);
			_sVService.changeValue(ownKey, getEnteredValue(), IEventBrokerBridge.Type.POST);
		}
		super.okPressed();
	}

	public T getSingleValue() {
		return _value.get();
	}
	

}
