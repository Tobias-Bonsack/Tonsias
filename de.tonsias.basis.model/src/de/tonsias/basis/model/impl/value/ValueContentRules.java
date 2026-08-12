package de.tonsias.basis.model.impl.value;

import java.util.Optional;
import java.util.regex.Pattern;

import de.tonsias.basis.model.enums.ValueContentType;

/**
 * What every value type will and will not read, in one place.
 * <p>
 * A single value and a list of the same content answer the identical question -
 * "would this become a value?" - and the dialogs ask it a third time to decide
 * whether OK is enabled. Written out per class that would be ten copies of five
 * rules; here it is five, and {@code SingleFloatValue.accepts} as well as
 * {@code MultiFloatValue.accepts} are one-liners that hold no rule of their own
 * and therefore cannot drift apart.
 * </p>
 */
public final class ValueContentRules {

	/** the decimal notation {@link ValueContentType#FLOAT} accepts as text */
	private static final Pattern DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?");

	/** the shape of a key - see {@link #accepts} */
	private static final Pattern KEY = Pattern.compile("[0-9a-z]+");

	private ValueContentRules() {
	}

	/**
	 * Whether this content type would read the text. This is the dialogs' question,
	 * asked of the type itself so there is no second rule that could drift.
	 * <ul>
	 * <li>{@link ValueContentType#STRING} takes every text there is.</li>
	 * <li>{@link ValueContentType#INTEGER} takes what {@link Integer#valueOf} takes:
	 * digits with an optional sign, and nothing outside the {@code int} range - a
	 * matter of "99999999999" being offered and then silently landing as 0. See
	 * <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/68">#68</a>.</li>
	 * <li>{@link ValueContentType#BOOLEAN} takes the two literals and nothing else -
	 * anything else is rejected instead of being folded into {@code false}, so a
	 * typo does not silently clear the value.</li>
	 * <li>{@link ValueContentType#FLOAT} takes decimal notation only.
	 * {@link Float#parseFloat} would also read "NaN", "Infinity", "1e5", "3f" and
	 * "0x1p3", none of which anybody types into a value field on purpose - and only
	 * what stays a finite number: a one followed by forty zeros passes the notation
	 * but parses to {@code Infinity}, which is the surprising number this type
	 * promises not to store. See
	 * <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/68">#68</a>.</li>
	 * <li>{@link ValueContentType#INSTANZ} takes the shape of a key: base 36, lower
	 * case only, as {@code KeyServiceImpl} hands them out. Whether an instanz of
	 * that key <em>exists</em> is a question this bundle cannot ask - the key
	 * service and the instanz service both live in {@code de.tonsias.basis.osgi},
	 * which depends on this one and not the other way round. A key that resolves to
	 * nothing is therefore accepted here and comes back empty from the service. The
	 * empty string is <em>not</em> accepted: nothing chosen must not become a value.
	 * {@link #convert} is the wider question and does take it.</li>
	 * </ul>
	 */
	public static boolean accepts(ValueContentType content, String text) {
		if (text == null) {
			return false;
		}
		return switch (content) {
		case STRING -> true;
		case INTEGER -> {
			try {
				Integer.valueOf(text);
				yield true;
			} catch (NumberFormatException e) {
				yield false;
			}
		}
		case BOOLEAN -> {
			String trimmed = text.strip();
			yield Boolean.TRUE.toString().equalsIgnoreCase(trimmed)
					|| Boolean.FALSE.toString().equalsIgnoreCase(trimmed);
		}
		case FLOAT -> {
			String number = text.strip();
			yield DECIMAL.matcher(number).matches() && Float.isFinite(Float.parseFloat(number));
		}
		case INSTANZ -> KEY.matcher(text).matches();
		default -> throw new IllegalArgumentException("Unexpected value: " + content);
		};
	}

	/**
	 * What this raw object becomes when stored, if anything - the model's question,
	 * asked by every {@code tryToSetValue} and {@code tryToAddValue}. Takes the
	 * already typed object as well as the text a widget hands over.
	 * <p>
	 * The one place this is wider than {@link #accepts} is
	 * {@link ValueContentType#INSTANZ}, which stores the empty string: pointing
	 * nowhere is a state a relation has - it is the one a fresh one starts in - and
	 * it is where a reference is put back to when its target is deleted, so the way
	 * in has to exist. What must not happen is a dialog offering it, which is why
	 * {@link #accepts} is the narrower question.
	 * </p>
	 *
	 * @return the value to store, or empty when this content type will not read it
	 */
	public static Optional<Object> convert(ValueContentType content, Object raw) {
		if (raw == null) {
			return Optional.empty();
		}
		return switch (content) {
		case STRING -> raw instanceof String text ? Optional.<Object>of(text) : Optional.empty();
		case INTEGER -> {
			if (raw instanceof Integer number) {
				yield Optional.of(number);
			}
			yield raw instanceof String text && accepts(content, text) ? Optional.of(Integer.valueOf(text))
					: Optional.empty();
		}
		case BOOLEAN -> {
			if (raw instanceof Boolean flag) {
				yield Optional.of(flag);
			}
			yield raw instanceof String text && accepts(content, text) ? Optional.of(Boolean.valueOf(text.strip()))
					: Optional.empty();
		}
		case FLOAT -> {
			if (raw instanceof Float number) {
				// the same rule as for text: what the type will not read, it will not store
				// from a caller that already holds the float either
				yield Float.isFinite(number) ? Optional.of(number) : Optional.empty();
			}
			yield raw instanceof String text && accepts(content, text) ? Optional.of(Float.valueOf(text.strip()))
					: Optional.empty();
		}
		case INSTANZ -> raw instanceof String text && (text.isEmpty() || accepts(content, text))
				? Optional.<Object>of(text)
				: Optional.empty();
		default -> throw new IllegalArgumentException("Unexpected value: " + content);
		};
	}
}
