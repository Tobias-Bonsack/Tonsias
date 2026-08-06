# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Tonsias is an **Eclipse RCP / e4 desktop application** developed as an **Eclipse PDE workspace** — one directory per OSGi bundle, plus two features, a product, and a target definition. Day-to-day development happens in the Eclipse IDE; a **Tycho build** mirrors it on the command line for CI and for verifying changes without the IDE.

## Commands

```bash
./mvnw clean verify                              # compile every bundle, run all test bundles, build the product
./mvnw clean verify -Dmaven.test.failure.ignore=true   # keep going past failing tests to see every result
./mvnw clean verify -pl de.tonsias.basis.osgi.test     # one test bundle (add -am to build its dependencies)
./mvnw clean verify -Dtest=KeyServiceImplTest          # a single test class
./mvnw clean verify -DskipTests                        # compile only
```

Requires a JDK 24 in `JAVA_HOME` (the bundles' highest BREE). The wrapper downloads Maven itself; no local Maven install is needed.

## Working in this repo

- **Target platform**: `target-platform/target-platform.target` — Eclipse SDK 4.36 (2025-06) + Maven-sourced Guava 33.1.0, Gson 2.10.1, JUnit Jupiter 5.14.4, Mockito 5.23.0. It is the single source of truth for **both** the IDE and the Tycho build (consumed as an `eclipse-target-definition` module), so edit it rather than the poms when changing dependencies. Set it as the active target platform in the IDE before anything resolves.
- **Run the app**: launch `de.tonsias.basis.product/tonsias.product` (E4Application, `-clearPersistedState`). Note `autoStart` config for `org.apache.felix.scr` and `org.eclipse.equinox.event` — DS and the event admin must be running or none of the services resolve. The Tycho test runtime configures the same two bundles for the same reason.
- **Tests** are JUnit 5, and all three bundles need an OSGi runtime — run them as an **Eclipse JUnit Plug-in Test** in the IDE, or via `./mvnw verify`. What differs is how they obtain their subject:
  - `BasicPreferenceServiceImplTest`, `KeyServiceImplTest`, `EventTreeNodeWrapperTest`, `InstanzViewLogicTest` construct it directly or with `@InjectMocks`, so they pass headlessly.
  - `InstanzServiceImplTest` and `SingleValueServiceImplTest` call `OsgiUtil.getService(...)`. These **cannot pass in a plain DS runtime**: `InstanzServiceImpl` has a mandatory `@Reference IEventBrokerBridge`, and that bridge is only produced by `EventBrokerContextFunction`, an e4 `IContextFunction` that runs only when an `IEclipseContext` requests the key. Headless, nothing requests it, so the component never activates and `getService` returns `null`. They need a running e4 application context. `InstanzServiceImplTest` also writes real files to the instance location.
- There are **no `.launch` files committed** — launch configs are local. If a launch config is missing, create one from the product / plug-in test wizard.
- Java compliance levels are **inconsistent across bundles**, and the BREE in `MANIFEST.MF` frequently disagrees with the compliance in `.settings/org.eclipse.jdt.core.prefs` — `de.tonsias.basis.ui` declares `JavaSE-24` but compiles at 19, `de.tonsias.basis.logic` declares 24 but compiles at 22, while `de.tonsias.basis.osgi` and `de.tonsias.basis.model` are 19/19. A language feature usable in `de.tonsias.basis.logic` (22) will not compile in `de.tonsias.basis.osgi` (19). When adding a bundle, copy the settings from a sibling rather than accepting the wizard default. This is also why the Tycho build has to pin its resolution EE (see the comment in `pom.xml`); normalising the BREEs would let that pin go away.

## Bundle layout and dependency direction

```
basis.model          POJOs, no OSGi deps (Guava BiMap only)
basis.data.access    Gson persistence: LoadService / SaveService / DeleteService (DS @Component)
basis.osgi           services — see the package split below
basis.logic          headless view logic (Eclipse Jobs), no SWT
basis.ui             e4 parts, handlers, dialogs, providers  ── Application.e4xmi lives here
basis.ui.i18n        Messages class + OSGI-INF/l10n bundles
basis.icon           IconUtil + res/*.png
delta.view.*         the Delta view feature: logic / ui (fragment.e4xmi) / ui.i18n / ui.test
```

`de.tonsias.basis.osgi` carries both contract and implementation, and the split is enforced by its `Export-Package`:

| Package | Contents | Visibility |
|---|---|---|
| `de.tonsias.basis.osgi.intf` | service interfaces (`IInstanzService`, `IKeyService`, `IDeltaService`, `IEventBrokerBridge`, …) | public |
| `de.tonsias.basis.osgi.intf.non.service` | event-topic constants and event payload records | public |
| `de.tonsias.basis.osgi.util` | `OsgiUtil` | public |
| `de.tonsias.basis.osgi.impl` | `*ServiceImpl`, context functions, `ChangePropagationListener` | `x-friends:="de.tonsias.basis.osgi.test"` |

The important rule: **UI and logic code depends on `…osgi.intf` only** — never on `…osgi.impl`, which is visible solely to the test bundle.

Note that the branch `feat-2-variable_path` (issue #2) moves the interface packages into a separate `de.tonsias.basis.osgi.interface` bundle. Once that merges, this section and the module list in `pom.xml` both need updating.

## Core architecture

### Model
`IInstanz` is the single tree node type: it has an own key, a parent key, a set of child keys, and per-`SingleValueType` `BiMap<valueKey, name>` maps of attributes. Attributes themselves are `ISingleValue` objects (`SingleStringValue`, `SingleIntegerValue`) living in their own files. **Everything is referenced by string key, never by object reference** — services resolve keys through a cache and fall back to loading from disk.

`IKeyService` generates keys as a base-62 counter (`KeyServiceImpl.KEYCHARS`, incrementing least-significant char first), persisted in Eclipse instance preferences. Key `"0"` is by convention the root instanz.

### Persistence
JSON via Gson, one file per object, written under `Platform.getInstanceLocation()`. The path is derived from the object itself: `ISavePathOwner.getPath()` + `getOwnKey()` + `.json` (e.g. `instanz/<key>.json`, `single_value/string/<key>.json`). Services cache loaded objects in a `Map<String, …>` and only touch disk on miss or save.

### Event flow — this is the heart of the app
Everything model-changing goes through the event bus, wrapped by `IEventBrokerBridge` (a thin façade over e4's `IEventBroker`, with `Type.SEND` = synchronous, `Type.POST` = async). **Every mutating service method takes an `IEventBrokerBridge.Type` parameter** and fires an event describing what changed.

- Topics and payloads are declared together in `de.tonsias.basis.osgi.intf.non.service.*EventConstants`. Payloads are `record`s (`InstanzEvent`, `ValueRenameEvent`, `LinkedChildChangeEvent`, `ParentChange`, `LinkedValueChangeEvent`, …) passed as `IEventBroker.DATA`. **When adding a topic, add its payload record next to it and register it in `KNOWN_DELTA`** or `DeltaServiceImpl` will throw `IllegalArgumentException` on save.
- `ChangePropagationListener` keeps both sides of every relation consistent: adding a child fires a child-list change, which the listener turns into a parent change on the other object, and vice versa. Its listeners re-enter the services with `Type.SEND`, so **a careless new listener can loop**; the services guard by checking "already in this state, return false without firing".
- `IDeltaService` (`DeltaServiceImpl`) subscribes to `instanz/delta/*` and `single_value/delta/*` and accumulates every event in `_notSavedEvents` since the last save. `saveDeltas()` folds that log into four key sets (instanz save/delete, single-value save/delete), calls the services, then clears the log back to a single `START_EVENT`. `EventConstants.OPEN_OPERATION`/`CLOSE_OPERATION` bracket an operation; `SAVE_ALL` triggers the save. The Delta view (`de.tonsias.delta.view.ui`) renders `getDeltas()` as a tree using those brackets.

### Dependency injection — three mechanisms coexist
1. **DS `@Component` + `@Reference`** for the plain services (`InstanzServiceImpl`, `KeyServiceImpl`, `SingleValueServiceImpl`, `BasicPreferenceServiceImpl`, the `data.access` services). Components are declared by **hand-maintained XML in `OSGI-INF/` referenced from `Service-Component:` in `MANIFEST.MF`** — adding a component means adding both. (`de.tonsias.basis.osgi/META-INF/MANIFEST.MF` currently lists two `OSGI-INF/de.tonsias.basis.osgi.util.*.xml` files that do not exist — stale entries from the interface refactor.)
2. **`ContextFunction`** for services that need the e4 `IEclipseContext`: `EventBrokerContextFunction` and `DeltaServiceContextFunction` build the impl with `ContextInjectionFactory.make(...)` and then register it in the OSGi registry, so the same instance is reachable both by `@Inject` in parts and by `OsgiUtil.getService(...)` in tests.
3. **`OsgiUtil.lazyLoading(Class, Consumer)`** for code that is constructed before OSGi is ready (see `ChangePropagationListener`) — a `ServiceTracker` calls back once the service appears.

### UI
e4 model-first: `de.tonsias.basis.ui/Application.e4xmi` defines the window, parts (`ModelView`, `InstanzView`), toolbars, and menus; `de.tonsias.delta.view.ui/fragment.e4xmi` contributes the Delta view as a model fragment. Parts are POJOs with `@PostConstruct postConstruct(Composite parent)` and field `@Inject` of services.

Non-trivial view behaviour is pushed into a separate `*.logic` bundle so it can be unit-tested without SWT — e.g. `InstanzView` (SWT) delegates to `InstanzViewLogic`, which debounces edits by scheduling Eclipse `Job`s in a serial `JobGroup` keyed by value key. Keep that split when adding view behaviour.

Views react to model changes via `@Inject @Optional` + `@UIEventTopic`/`@EventTopic` methods rather than polling.

### i18n
Two levels, don't mix them up:
- **e4 model labels** (`%part.modelview` in the `.e4xmi`) resolve against `OSGI-INF/l10n/bundle.properties` / `bundle_de.properties` in the bundle that owns the model file.
- **Java strings** use `@Inject @Translation Messages _messages` — `Messages` is a plain class of public `String` fields whose names must match keys in its bundle's `OSGI-INF/l10n` properties. Adding a string means adding a field *and* the key in every locale file.

## Conventions

- Fields are prefixed with `_` (`_instanzService`, `_key`); record components too (`_parentKey`). Static constants are `UPPER_SNAKE`.
- Interfaces: `I*` for OSGi services (`IInstanzService`) — except the `data.access` ones, which are unprefixed (`LoadService`). Abstract bases are `A*` (`AInstanz`, `ASingleValue`, `AValueDialog`).
- Adding a bundle requires three edits beyond the project itself: `Require-Bundle` in the consuming manifests, a `<plugin>` entry in the owning `feature.xml`, and (for exported packages consumed only by tests) `x-friends` on the `Export-Package`.
- Commits reference GitHub issues: `feat #12: …`, `fix #14: …`, `fix(#20): …`. Branches are named `feat-<issue>-<slug>`.
- `de.tonsias.delta.view.ui/src/test.java` is an untracked scratch file, not part of the build — ignore it.

## Full workflow

Every feature follows this sequence end to end:

1. Switch to `main`.
2. Pull `main`.
3. Create a new dedicated branch off `main` (`feat-<issue>-<slug>`).
4. Make commits on that branch (`feat #<issue>: …` / `fix #<issue>: …`).
5. Run all tests and fix the failures — unless the failure is not caused by your changes.
6. Write new tests for the new implementations.
7. Push the branch and open a PR into `main`.

Never commit directly to `main`.

Step 5 is `./mvnw clean verify`. Note that this is **currently red on `main`** for pre-existing reasons (see [Working in this repo](#working-in-this-repo)): 18 of 44 tests fail — the 14 `OsgiUtil.getService` ones that need an e4 context, plus 4 in `logic.test` whose expectations have drifted from the production code. Compare against that baseline rather than assuming a red build is your fault, and never "fix" it by weakening assertions.

Step 6 places tests in the test bundle matching the layer under test: view logic → `de.tonsias.basis.logic.test`, services → `de.tonsias.basis.osgi.test`, delta view → `de.tonsias.delta.view.ui.test`. All three run inside OSGi; prefer `@InjectMocks`/direct construction over `OsgiUtil.getService(...)`, which only resolves under a running e4 context. A new bundle's internals need `x-friends` on its `Export-Package` before its test bundle can see them.
