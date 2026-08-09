package de.tonsias.basis.logic.part;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;

/**
 * The instanzen a relation can point at, in the order the model tree holds them,
 * each with the label it carries in the Model View.
 * <p>
 * This is what a {@code SingleInstanzValue} is chosen from, and it lives here
 * rather than next to the widget so it can be tested without SWT. The target is
 * not filtered: a reference to the instanz itself is no parent-child edge and so
 * cannot build a cycle in the tree.
 * </p>
 */
public class InstanzChoices {

	/**
	 * @param _key   of the instanz, which is what a reference stores
	 * @param _label how it reads in the Model View
	 */
	public record Choice(String _key, String _label) {
	}

	private final IInstanzService _instanzService;

	private final ISingleValueService _singleValueService;

	private final IBasicPreferenceService _preferenceService;

	public InstanzChoices(IInstanzService instanzService, ISingleValueService singleValueService,
			IBasicPreferenceService preferenceService) {
		_instanzService = instanzService;
		_singleValueService = singleValueService;
		_preferenceService = preferenceService;
	}

	/**
	 * Walks the tree from the root down, depth first, so the list reads like the
	 * Model View. Children that do not resolve are skipped rather than reported -
	 * the same thing the tree does when it builds its nodes.
	 * <p>
	 * Every instanz of the model, flat: a combo box is what this fills, and a large
	 * model makes it a long one, see
	 * <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/76">#76</a>.
	 * </p>
	 *
	 * @return every reachable instanz, the root first
	 */
	public List<Choice> choices() {
		List<Choice> result = new ArrayList<>();
		// a child key claiming an ancestor would otherwise walk forever. The services
		// keep parent and child in sync, but nothing stops a hand-edited file
		collect(_instanzService.getRoot(), result, new LinkedHashSet<>());
		return result;
	}

	private void collect(IInstanz instanz, List<Choice> result, Set<String> seen) {
		if (instanz == null || !seen.add(instanz.getOwnKey())) {
			return;
		}
		result.add(new Choice(instanz.getOwnKey(), labelOf(instanz)));
		for (String childKey : List.copyOf(instanz.getChildren())) {
			_instanzService.resolveKey(childKey).ifPresent(child -> collect(child, result, seen));
		}
	}

	/**
	 * How an instanz reads: the content of the string value the
	 * {@code MODEL_VIEW_TEXT} preference names, and its {@code toString()} when the
	 * preference is unset or names nothing this instanz carries. The Model View
	 * labels its nodes by the same rule, and this is the one place it is written -
	 * a reference that read differently there and here would be hard to follow.
	 */
	public String labelOf(IInstanz instanz) {
		Optional<String> parameterName = _preferenceService
				.getValue(IBasicPreferenceService.Key.MODEL_VIEW_TEXT.getKey(), String.class);
		if (parameterName.isEmpty()) {
			return instanz.toString();
		}

		String valueKey = instanz.getSingleValues(SingleValueType.SINGLE_STRING).inverse().get(parameterName.get());
		return _singleValueService
				.resolveKey(SingleValueType.SINGLE_STRING.getPath(), valueKey, SingleStringValue.class)
				.map(SingleStringValue::getValue)//
				.orElseGet(instanz::toString);
	}
}
