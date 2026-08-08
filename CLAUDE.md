# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Tonsias is an **Eclipse RCP / e4 desktop application** developed as an **Eclipse PDE workspace**. Day-to-day development happens in the Eclipse IDE; a **Tycho build** mirrors it on the command line for CI and for verifying changes without the IDE.

## Commands

```powershell
.\build.ps1                                            # the whole pipeline: compile, test, materialize the product
.\build.ps1 -SkipTests                                 # product only, no tests
.\build.ps1 -- -Dtest=KeyServiceImplTest               # everything after -- goes to Maven verbatim
```

`build.ps1` is the entry point to prefer, because `JAVA_HOME` is usually not set on a dev machine: it finds a JDK 24 (`C:\dev\java`, `%ProgramFiles%\Java`, Adoptium, …), exports it and then runs the wrapper. It fails fast if it finds nothing new enough. Underneath it is plain Maven, so with a JDK 24 in `JAVA_HOME` the wrapper works directly too:

```bash
./mvnw clean verify                                    # compile every bundle, run all test bundles, build the product
./mvnw clean verify -Dmaven.test.failure.ignore=true   # keep going past failing tests to see every result
./mvnw clean verify -Dtest=KeyServiceImplTest          # a single test class
./mvnw clean verify -DskipTests                        # compile only
```

In PowerShell, **quote any `-D` that contains a dot**: `'-Dmaven.test.failure.ignore=true'`. Unquoted, the parser splits it into `-Dmaven` and `.test.failure.ignore=true`, and Maven fails with "Unknown lifecycle phase". `-Dtest=…` and `-DskipTests` have no dot and are safe either way.

JDK 24 is the bundles' highest BREE and the resolution EE the target platform is pinned to; anything older fails to resolve Eclipse 4.36. The wrapper downloads Maven itself; no local Maven install is needed.

**Do not use `-pl` to build a single module.** Tycho derives the reactor from the OSGi manifests, not from Maven dependencies, so `-pl <module> -am` does not pull in the bundles that module requires and fails with "Missing requirement". Build the whole reactor and narrow with `-Dtest=` instead.

### Build output

`de.tonsias.basis.product` has `eclipse-repository` packaging, which by itself only publishes a p2 repository. `tycho-p2-director-plugin` (configured in its pom) additionally _installs_ the `tonsias` product out of that repository, which is what produces a runnable application:

| Path under `de.tonsias.basis.product/target`      | What it is                                        |
| ------------------------------------------------- | ------------------------------------------------- |
| `products/tonsias/win32/win32/x86_64/Tonsias.exe` | the launcher — run this to start the built app    |
| `products/tonsias/win32/win32/x86_64/jre`         | the bundled Java runtime, built by `jlink`        |
| `products/tonsias-win32.win32.x86_64.zip`         | the same directory zipped, the distributable      |
| `de.tonsias.basis.product-<version>.zip`          | the p2 repository, for installing/updating via p2 |

The product declares no `<vm>`, so nothing writes a `-vm` into `Tonsias.ini` and the native launcher has to find a JVM itself: `-vm` argument, `-vm` in the ini, a `jre` directory next to the launcher, then the `PATH`. On a machine without `java` on the `PATH` — the normal case here — it used to fail at step four *silently*: exit code 1, no output, no workspace. A `maven-antrun-plugin` execution in `de.tonsias.basis.product/pom.xml` therefore runs `jlink` into that `jre` directory, which makes the installation self-contained and stops the search at step three. Two things follow from it:

- The module list is a property, `tonsias.jre.modules`: `java.se` plus every non-tool `jdk.*` module. Equinox derives the packages of the system bundle from the modules actually in the image, so leaving one out is the same as removing a package — without `jdk.xml.dom` there is no `org.w3c.dom.css` and `org.eclipse.e4.ui.css.swt` does not resolve.
- `jlink` has to run after `materialize-products` and before `archive-products`, so it sits at `pre-integration-test` and `archive-products` moved off its default phase to `verify`. `mvn package` alone now leaves the product unarchived; `verify` (what `build.ps1` and CI run) is unaffected.

Only `win32/win32/x86_64` is built; add `<environment>`s to `target-platform-configuration` in the parent pom to cross-build others — each one needs its own `jlink` run from a JDK for that platform.

`.github/workflows/build.yml` is the only workflow: it runs the same `./mvnw clean verify` on `windows-latest` for every push and PR to `main`, and it must stay on a Windows runner as long as the target platform declares only the win32 environment. Three things about it are deliberate:

- It builds with **`-Dmaven.test.failure.ignore=true`**, so one failing bundle does not hide the results of the bundles behind it in the reactor. That only moves _when_ a failure is reported, not _whether_ — the last step fails the job from the parsed totals.
- `.github/scripts/Summarize-TestResults.ps1` folds all `target/surefire-reports/TEST-*.xml` into a markdown table, posted on a PR as a **single, edited-in-place comment** (found again by the `<!-- tonsias-test-report -->` marker) and written to the job summary. It is assembled from the surefire XML, so a new test bundle shows up by itself — as long as its reports land under `<bundle>/target/surefire-reports`. Run it locally after `.\build.ps1` for the same report.
- Product and p2 repository are uploaded as artifacts **only from a fully green run**. `maven.test.failure.ignore` makes Maven exit 0 on a failing test, so `if: success()` alone is no longer enough and the upload steps check the report's `failed` output as well.

## Working in this repo

- **Target platform**: `target-platform/target-platform.target` — Eclipse SDK 4.36 (2025-06) + Maven-sourced Guava 33.1.0, Gson 2.10.1, JUnit Jupiter 5.14.4, Mockito 5.23.0. It is the single source of truth for **both** the IDE and the Tycho build (consumed as an `eclipse-target-definition` module), so edit it rather than the poms when changing dependencies. Set it as the active target platform in the IDE before anything resolves.
- **Run the app**: launch `de.tonsias.basis.product/tonsias.product` (E4Application, `-clearPersistedState`). Note `autoStart` config for `org.apache.felix.scr` and `org.eclipse.equinox.event` — DS and the event admin must be running or none of the services resolve. The Tycho test runtime configures the same two bundles for the same reason.
- **Tests** are JUnit 5, and all three bundles need an OSGi runtime — run them as an **Eclipse JUnit Plug-in Test** in the IDE, or via `./mvnw verify`. What differs is how they obtain their subject:
  - Most tests construct it directly or with `@InjectMocks` and need nothing further.
  - Tests that resolve real services through `OsgiUtil.getService(...)` (`InstanzServiceImplTest`, `SingleValueServiceImplTest`, `OsgiUtilTest`) must first call **`E4ServiceContext.prime()`** in `@BeforeEach`. `IEventBrokerBridge` and `IDeltaService` are not plain DS components — they come from `IContextFunction`s that only run when an `IEclipseContext` is asked for their key, and `ChangePropagationListener` is contributed as an e4 _addon_ in `Application.e4xmi`. In the product the workbench triggers all three; headless nothing does, so without priming `InstanzServiceImpl`'s mandatory `@Reference IEventBrokerBridge` stays unsatisfied and the component never activates. Add `prime()` to any new test that touches real services.
  - Those tests also need a **root instanz** (`_inse.getRoot()`), which `ModelView` creates at start-up in the product but nothing creates in a fresh test workspace. They write real files to the instance location (`target/work/data`), which is cleaned per run.
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
delta.view.*         the Delta view feature: logic / ui (fragment.e4xmi) / ui.test
```

`de.tonsias.basis.osgi` carries both contract and implementation, and the split is enforced by its `Export-Package`:

| Package                                  | Contents                                                                                        | Visibility                                |
| ---------------------------------------- | ----------------------------------------------------------------------------------------------- | ----------------------------------------- |
| `de.tonsias.basis.osgi.intf`             | service interfaces (`IInstanzService`, `IKeyService`, `IDeltaService`, `IEventBrokerBridge`, …) | public                                    |
| `de.tonsias.basis.osgi.intf.non.service` | event-topic constants and event payload records                                                 | public                                    |
| `de.tonsias.basis.osgi.util`             | `OsgiUtil`                                                                                      | public                                    |
| `de.tonsias.basis.osgi.impl`             | `*ServiceImpl`, context functions, `ChangePropagationListener`                                  | `x-friends:="de.tonsias.basis.osgi.test"` |

The important rule: **UI and logic code depends on `…osgi.intf` only** — never on `…osgi.impl`, which is visible solely to the test bundle.

## Core architecture

### Model

`IInstanz` is the single tree node type: it has an own key, a parent key, a set of child keys, and per-`SingleValueType` `BiMap<valueKey, name>` maps of attributes. Attributes themselves are `ISingleValue` objects living in their own files. **Everything is referenced by string key, never by object reference** — services resolve keys through a cache and fall back to loading from disk.

`IKeyService` generates keys as a base-36 counter (`KeyServiceImpl.KEYCHARS`, incrementing least-significant char first), persisted in Eclipse instance preferences. Key `"0"` is by convention the root instanz. The alphabet is **lower case only and must stay sorted ascending**.

### Persistence

JSON via Gson, one file per object, written under `Platform.getInstanceLocation()`. The path is derived from the object itself: `ISavePathOwner.getPath()` + `getOwnKey()` + `.json` (e.g. `instanz/<key>.json`, `single_value/string/<key>.json`). Services cache loaded objects in a `Map<String, …>` and only touch disk on miss or save.

### Event flow — this is the heart of the app

Everything model-changing goes through the event bus, wrapped by `IEventBrokerBridge` (a thin facade over e4's `IEventBroker`, with `Type.SEND` = synchronous, `Type.POST` = async). **Every mutating service method takes an `IEventBrokerBridge.Type` parameter** and fires an event describing what changed.

- Topics and payloads are declared together in `de.tonsias.basis.osgi.intf.non.service.*EventConstants`. Payloads are `record`s passed as `IEventBroker.DATA`. **When adding a topic, add its payload record next to it and register it in `KNOWN_DELTA`** or `DeltaServiceImpl` will throw `IllegalArgumentException` on save.
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
- **Java strings** use `@Inject @Translation Messages _messages` — `Messages` is a plain class of public `String` fields whose names must match keys in its bundle's `OSGI-INF/l10n` properties. Adding a string means adding a field _and_ the key in every locale file.

`de.tonsias.basis.ui.i18n` is the only `Messages` bundle; the Delta view has no Java strings of its own and gets its labels purely from the model level. Both levels are guarded by tests, so a forgotten locale fails the build rather than showing up as a raw key at runtime: `TranslationCoverageTest` (in `de.tonsias.basis.ui.test`) matches the `Messages` fields against both locale files and the `%keys` of `Application.e4xmi` against `de.tonsias.basis.ui`'s, `FragmentTranslationCoverageTest` does the same for `fragment.e4xmi`. Keep the German files in `\uXXXX` escapes — they are read as ISO-8859-1 by `Properties.load`.

Not everything that reads like text is UI text: `Job`/`JobGroup` names in `*.logic` and in the handlers are diagnostic labels. The window has no trim bar and therefore no progress reporting, so they are never displayed and deliberately stay untranslated. `IObject.toString()` (the tree labels of both views) and the preference *node* paths in the preferences dialog are identifiers, not labels — the preference *keys* next to the fields are translated through `MessagesUtil.getPreferenceLabel`, which also maps `SingleValueType` onto its label so the enum name never reaches the screen.

## Conventions

- Fields are prefixed with `_` (`_instanzService`, `_key`); record components too (`_parentKey`). Static constants are `UPPER_SNAKE`.
- Interfaces: `I*` for OSGi services (`IInstanzService`) — except the `data.access` ones, which are unprefixed (`LoadService`). Abstract bases are `A*` (`AInstanz`, `ASingleValue`, `AValueDialog`).
- Adding a bundle requires three edits beyond the project itself: `Require-Bundle` in the consuming manifests, a `<plugin>` entry in the owning `feature.xml`, and (for exported packages consumed only by tests) `x-friends` on the `Export-Package`.

## Full workflow

Every feature follows this sequence end to end:

1. Switch to `main`.
2. Pull `main`.
3. Create a new dedicated branch off `main` (`feat-<name>`).
4. Make commits on that branch (`feat <name>: …` / `fix <name>: …`).
5. Run all tests and fix the failures — unless the failure is not caused by your changes.
6. Write new tests for the new implementations.
7. Push the branch and open a PR into `main`.

Never commit directly to `main`.

Step 5 is `.\build.ps1` (or `./mvnw clean verify`). The suite is **green on `main`** — 303 tests across the six test bundles, all passing — so any failure is yours. Never "fix" one by weakening an assertion; if a test looks wrong, check it against the production code first (several once verified `post(..)` where the code had moved to `send(..)`).

Step 6 places tests in the test bundle matching the layer under test. All run inside OSGi; prefer `OsgiUtil.getService(...)`, which only resolves under a running e4 context, over `@InjectMocks` direct construction. A new bundle's internals need `x-friends` on its `Export-Package` before its test bundle can see them.

## Releasing

The latest release is **0.1.0** (the first one) and the reactor is on **0.2.0-SNAPSHOT** behind it; `CHANGELOG.md` is the per-release record and `README.md` the user-facing overview. Versions live in 37 files — every `pom.xml`, `MANIFEST.MF`, `feature.xml` and the `.product` — so never edit them by hand. Bump them with Tycho, which also rewrites the `bundle-version` lower bounds in the `Require-Bundle` of every other reactor bundle:

```powershell
$env:JAVA_HOME = '<jdk24>'
.\mvnw.cmd org.eclipse.tycho:tycho-versions-plugin:5.0.3:set-version "-DnewVersion=<x.y.z>" "-DupdateVersionRangeMatchingBounds=true"
```

It leaves the `version` attribute of `tonsias.product` alone when that attribute does not already match the old version — check it afterwards. Then run `.\build.ps1`, add a `CHANGELOG.md` section, and follow the branch/PR workflow above; the tag is `v<x.y.z>` on the merge commit, with the product zip and the p2 repository from `de.tonsias.basis.product/target` attached to the GitHub release.
