# Changelog

All notable changes to Tonsias are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[semantic versioning](https://semver.org/spec/v2.0.0.html). While the major version is
`0`, anything may still change — including the on-disk format of the model.

## [Unreleased]

Development towards 0.2.0. The reactor is at `0.2.0-SNAPSHOT`.

### Added

- `SingleBooleanValue`, a third attribute type for yes-or-no attributes, alongside
  string and integer. It is created from the model view or while creating an instanz,
  shown and toggled as a check box in the instanz view, and stored under
  `single_value/boolean/`. Only the two literals `true` and `false` are accepted, so a
  typo is rejected rather than silently clearing the value.
- `SingleFloatValue`, a fourth attribute type for decimal numbers, alongside string,
  integer and boolean. It is created from the model view or while creating an instanz,
  edited in the instanz view, and stored under `single_value/float/`. Only decimal
  notation is accepted — `NaN`, `Infinity`, `1e5` and the German `3,14` are rejected
  instead of turning into a number nobody typed, and the dialog greys out its OK button
  on exactly that rule.
- `SingleInstanzValue`, a fifth attribute type, and the first that is a *relation*: it
  points at another instanz. What it stores is the target's key — the model references
  everything by key, and resolving one needs a service the model bundle deliberately
  cannot reach — so it is persisted by the same Gson as every other value, under
  `single_value/instanz/`, and no new event topic was needed. It is created from the
  model view and edited in the instanz view by choosing from a list of the model's
  instanzen rather than by typing, and `IInstanzService.resolveInstanzValue` follows it
  back to the instanz. A reference whose target has been deleted keeps its key and
  resolves to nothing ([#74](https://github.com/Tobias-Bonsack/Tonsias/issues/74)).

### Changed — tests

- Every test is now a system test. The mock-based tests have been rewritten to drive the
  services the running application registers, on the real event bus, against the real
  workspace files, and — for the Delta view — on a real SWT `Display`. A call is judged
  by the state it leaves behind and the events that left the bus, not by what a
  collaborator was seen being told.
- All test classes moved into a `…test.system` package of their bundle, and the ones
  that only differed by their subject were merged: `InstanzServiceSystemTest`,
  `SingleValueServiceSystemTest`, `ChangePropagationSystemTest`, `DeltaLogSystemTest`,
  `EventBrokerBridgeSystemTest`, `KeyServiceSystemTest`,
  `BasicPreferenceServiceSystemTest`, `InstanzViewLogicSystemTest`,
  `CreateInstanzDialogLogicSystemTest`, `PreferencesDialogLogicSystemTest`,
  `DeltaTreeSystemTest`, `DeltaViewSystemTest`.
- `de.tonsias.basis.osgi.test` now exports a shared harness (`x-friends` to the logic and
  delta-view test bundles): `ProductRuntime` brings the runtime up and reads objects back
  off disk, and `EventRecorder` collects what passes on the bus.
- The persistence tests take `LoadService`, `SaveService` and `DeleteService` from the
  service registry instead of constructing them.

### Removed

- Mockito, from every bundle manifest and from the target platform. Nothing uses a
  mocking framework any more.
- The all-fields constructor of `AInstanz`. Nothing ever called it: instanzen are built
  with `new Instanz(key)` and filled through `addValuekeys(..)`, and Gson constructs the
  class without any constructor. It grew a parameter with every new value type, and
  whoever had used it would have bypassed the lazy map creation in `getSingleValues` and
  could have left a map at `null` ([#61]).
- The second constructor of `AInstanz`, the one taking a key and a parent key. `Instanz`
  is its only subclass and passes on the key alone, so nothing could reach it — and a
  parent set there would have been set past `IInstanzService`, without the other side
  being pulled along and without a delta. `createInstanz(parentKey, Type)` is the way
  ([#65]).

### Fixed

- The e4 context functions hand out one shared service instance instead of building a
  new one on every `compute(..)`. An e4 context caches per *asking* context, so the
  Delta view used to render a different delta log than the one the save path empties
  ([#52]).
- `saveDeltas()` empties its log whatever happens: the reset moved into a `finally`, a
  failing step is collected as a suppressed exception rather than ending the save, and a
  delete that finds no file counts as done. One bad delete used to wedge every save that
  followed ([#53]).
- `DeltaServiceImpl` no longer depends on the order in which its fields happen to be
  injected. `DeltaServiceContextFunction` asks the context for `IEventBrokerBridge`
  before it builds, so the services that need the bridge are activated by the time their
  turn comes, whatever order `Class#getDeclaredFields()` returns ([#56]).
- `CLAUDE.md` no longer claims that two `OSGI-INF` component descriptions are missing —
  they are there, and were all along ([#57]).
- The OK button of a value dialog is now set from the dialog's own validation the
  moment the dialog is built, not only from the first keystroke on. A dialog opened
  for a new integer or float value used to offer OK over its empty field — pressing it
  created a value whose input `tryToSetValue` then rejected, silently leaving it at `0`
  or `0.0`. The rule lives in `AValueDialog.isValueAcceptable()` now, so every dialog
  answers for its own type in one place ([#62]).
- The name check and the value check of a value dialog no longer overwrite each other's
  verdict at the OK button. Both are asked in `AValueDialog.refreshOkButton()`, the one
  place that sets it: a value the model would discard stayed discarded when the name is
  touched, and a name another value already carries stays refused when the value is.
  The name a stored value carries counts as its own, so editing it is not blocked
  against itself ([#67]).
- The number dialogs ask their type what it takes instead of matching a pattern of their
  own. `SingleIntegerValue.accepts(..)` and `SingleFloatValue.accepts(..)` are the one
  rule each, and `tryToSetValue` applies it itself: `99999999999` is no longer offered
  and then silently stored as `0`, `+5` is no longer refused although the model reads
  it, and a decimal number too big for a `float` is rejected instead of being stored as
  the `Infinity` the type promises never to hold ([#68]).
- The reason a value dialog refuses a name is now on a label under the field, where it
  can be read. It used to be handed to `Text.setMessage(..)`, the placeholder SWT only
  draws over an empty field — so in the one case it was set for, a name someone had just
  typed, it was never on screen and the red field stood there without a word ([#71]).
- A value dialog opened on a stored value writes the value back, not only the name. The
  field was prefilled, editable and read for the OK button, and its content was then
  dropped on OK. `okPressed()` moved up into `AValueDialog`, which is where the four
  dialogs only differed in how their widget is read ([#72]).

[#52]: https://github.com/Tobias-Bonsack/Tonsias/issues/52
[#53]: https://github.com/Tobias-Bonsack/Tonsias/issues/53
[#56]: https://github.com/Tobias-Bonsack/Tonsias/issues/56
[#57]: https://github.com/Tobias-Bonsack/Tonsias/issues/57
[#61]: https://github.com/Tobias-Bonsack/Tonsias/issues/61
[#62]: https://github.com/Tobias-Bonsack/Tonsias/issues/62
[#65]: https://github.com/Tobias-Bonsack/Tonsias/issues/65
[#67]: https://github.com/Tobias-Bonsack/Tonsias/issues/67
[#68]: https://github.com/Tobias-Bonsack/Tonsias/issues/68
[#71]: https://github.com/Tobias-Bonsack/Tonsias/issues/71
[#72]: https://github.com/Tobias-Bonsack/Tonsias/issues/72

## [0.1.0] - 2026-08-07

The first release. It is the point at which the model, the persistence, the event chain
and the user interface work together end to end, and at which the whole application can
be built and tested from the command line. Everything below is therefore new; the list
is a summary of what the release contains rather than of what changed.

### Added — model and persistence

- `IInstanz` as the single tree node type: an own key, a parent key, a set of child keys
  and, per value type, a bidirectional map from value key to value name.
- `ISingleValue` attributes in two types, `SingleStringValue` and `SingleIntegerValue`,
  each living in its own object.
- Objects are addressed by string key throughout. The services resolve a key through a
  cache and fall back to loading from disk on a miss.
- Gson persistence with one JSON file per object under the Eclipse instance location:
  `instanz/<key>.json`, `single_value/string/<key>.json` and
  `single_value/integer/<key>.json`. Load, save and delete are separate services.
- Key generation as a base-36 counter over a lower-case alphabet, persisted in the
  Eclipse instance preferences. Key `0` is by convention the root Instanz.

### Added — the event chain

- `IEventBrokerBridge`, a facade over the e4 `IEventBroker` with an explicit
  synchronous (`SEND`) and asynchronous (`POST`) mode. Every mutating service method
  takes that mode and fires an event describing what it changed.
- Event topics for both object kinds — new, name change, value change, child list
  change, parent change, value list change, delete — each with its own payload record.
- `ChangePropagationListener` keeps both sides of every relation consistent: adding a
  child sets the parent on the other object and vice versa. The services guard against
  the resulting re-entry by not firing when a change is a no-op.
- `IDeltaService` accumulates every model event since the last save, brackets them per
  operation, and on save folds the log into four key sets — Instanzen and values to
  save, Instanzen and values to delete — before calling the services.

### Added — user interface

- **Model View**: the model as a virtual tree of Instanzen and their values, with a
  context menu (also opened with `Ctrl+N`) to create a child Instanz, add a string or
  integer single value through a dialog, or delete a value. The tree updates from model
  and preference events.
- **Property View**: own key, parent key, child keys and all single values of the
  selected Instanz, grouped by type. Values and value names are edited in place, with
  colour feedback for modified and to-be-deleted fields, a dirty marker on the part,
  `Ctrl+S` to write, `F5` to reload, and a prompt when leaving a dirty Instanz.
- **Delta View**: all unsaved changes as a tree grouped per operation, with toolbar
  actions to refresh and to save everything.
- Saving from `File > Save`, the window toolbar and `Ctrl+S`.
- Edits in the Property View are debounced through Eclipse `Job`s in a serial job group
  per value key, so a burst of keystrokes does not become a burst of writes.
- Preferences dialog under `Window > Preferences` for the value name shown in the Model
  View, whether values appear in the tree, and the model root path (read-only, click to
  copy).
- Non-trivial view behaviour lives in separate `*.logic` bundles without SWT, so it can
  be tested headlessly.
- English and German translations for both the e4 model labels and the Java strings.

### Added — build and quality

- A Tycho build that mirrors the Eclipse PDE workspace: it compiles every bundle,
  runs every test bundle inside a real Equinox with Declarative Services and the event
  admin started, and installs the product with `tycho-p2-director-plugin`.
- `build.ps1` as the entry point on Windows: it locates a JDK 24, exports `JAVA_HOME`
  and runs the Maven wrapper, so no local Maven or preset `JAVA_HOME` is needed.
- `target-platform/target-platform.target` as the single target definition for the IDE
  and the build — Eclipse SDK 4.36 plus Guava, Gson, JUnit Jupiter and Mockito.
- 282 tests across five test bundles, covering the model, the persistence, the services,
  the view logic and the complete event chain.
- A GitHub Actions workflow that builds every push and pull request on Windows, folds
  all surefire reports into one markdown table, posts it as a single edited-in-place
  pull request comment, and uploads the product and the p2 repository from green runs.

### Distribution

- `tonsias-win32.win32.x86_64.zip` — the runnable application for Windows x86_64.
  Java 24 or newer must be on the `PATH`; no JVM is bundled.
- `de.tonsias.basis.product-0.1.0.zip` — the p2 repository, for installing or updating
  from Eclipse.

### Known limitations

- Only a Windows x86_64 product is built. Other platforms need additional
  `<environment>` entries in `target-platform-configuration`.
- Single values are limited to string and integer.
- Instanzen can be created but not deleted from the user interface.
- The bundles declare inconsistent Java compliance levels (19 / 22 / 24), which is why
  the build pins its resolution execution environment to `JavaSE-24`.

[Unreleased]: https://github.com/Tobias-Bonsack/Tonsias/compare/v0.1.0...main
[0.1.0]: https://github.com/Tobias-Bonsack/Tonsias/releases/tag/v0.1.0
