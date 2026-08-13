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

import de.tonsias.basis.model.enums.IValueType;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.IValue;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.IKeyService;
import de.tonsias.basis.osgi.util.OsgiUtil;
import de.tonsias.basis.ui.i18n.Messages;

/**
 * Everything a value dialog does that is not "one value": the instanz side with
 * the key and the name, the red line saying why a name is refused, the separator,
 * and the OK button that is only on while both halves are content.
 * <p>
 * {@link AValueDialog} adds the one value below it, {@link AMultiValueDialog} the
 * list.
 * </p>
 *
 * @param <V> the edited value
 * @param <C> the SWT widget this value is entered with - the subclass names it,
 *            so it works on its own widget without casting
 */
public abstract class AValueDialogBase<V extends IValue, C extends Control> extends Dialog {

	IKeyService _keyService = OsgiUtil.getService(IKeyService.class);

	IInstanzService _iService = OsgiUtil.getService(IInstanzService.class);

	// empty while a new value is being created, and set by okPressed once it is.
	// Present means an existing value is being edited
	Optional<V> _value;

	IInstanz _instanz;

	Text _nameText;

	Label _nameMessage;

	C _valueControl;

	IValueType _type;

	protected Messages _messages;

	protected AValueDialogBase(Shell parentShell, V value, IInstanz parent, Messages messages) {
		super(parentShell);
		_messages = messages;
		_value = Optional.ofNullable(value);
		_instanz = parent;
		_type = getType();
	}

	abstract IValueType getType();

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

		createValuePart(composite);

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
	 * Whether the value control currently holds something the type would take. The
	 * base answer is yes - a text field of any content is a string, and a check box
	 * cannot hold an invalid state; the numeric dialogs and the list put the
	 * question to their type.
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
		String owner = _instanz.getValues(_type).inverse().get(_nameText.getText());
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

		String name = _instanz.getValues(_type).getOrDefault(keyString, "");
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
	protected void refreshNameMessage() {
		boolean acceptable = isNameAcceptable();
		_nameText.setBackground(acceptable ? null : Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		_nameMessage.setText(acceptable ? "" : _messages.dialog_value_usedName);
	}

	private void createValuePart(Composite composite) {
		Label sideLabel = LabelFactory.newLabel(SWT.None).text(_messages.dialog_value_valueSide).create(composite);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(sideLabel);

		Label valueLabel = LabelFactory.newLabel(SWT.None).text(valueSideLabel()).create(composite);
		GridDataFactory.fillDefaults().applyTo(valueLabel);

		_valueControl = createValueControl(composite, valuePrefill());
	}

	/** what the value control is called - one value, or the elements of a list */
	protected String valueSideLabel() {
		return _messages.constant_singleValue;
	}

	/** what the value control opens with, empty for a value being created */
	protected String valuePrefill() {
		return "";
	}

	/**
	 * Creates the widget the value is entered with. The subclass decides its type
	 * and prefills it from {@code valuePrefill}, which is empty for a new value.
	 */
	protected abstract C createValueControl(Composite composite, String valuePrefill);

	/**
	 * The type this dialog edits, as the class {@code createNew} is handed. It is
	 * the one in {@link #getType()}, typed, so what comes back fits {@code _value}.
	 */
	protected abstract Class<V> getValueClass();

	/** creates the value, or writes back both halves of the one being edited */
	protected abstract void persist();

	@Override
	protected void okPressed() {
		persist();
		super.okPressed();
	}

	/** the value this dialog created or edited */
	public V getCreatedValue() {
		return _value.get();
	}

	/**
	 * The widget the value is entered with, so a test can drive it the way a user
	 * would rather than reach past it into the dialog.
	 */
	public C getValueControl() {
		return _valueControl;
	}
}
