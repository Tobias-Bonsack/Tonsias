# CLAUDE.md

Guidance for Claude Code (claude.ai/code) in this repository.

Tonsias is an **Eclipse RCP / e4 desktop application** in an **Eclipse PDE workspace**. A **Tycho build** mirrors the IDE on the command line for CI.

## Commands

```powershell
.\build.ps1                                    # compile, test, materialize the product
.\build.ps1 -SkipTests                         # product only
.\build.ps1 -- -Dtest=KeyServiceSystemTest     # everything after -- goes to Maven verbatim
```

Prefer `build.ps1`: it locates a JDK 24, exports `JAVA_HOME` (usually unset here) and runs the wrapper. With a JDK 24 already in `JAVA_HOME`, `./mvnw clean verify` works directly. JDK 24 is the highest BREE and the pinned resolution EE — anything older fails to resolve Eclipse 4.36.

- In PowerShell, **quote any `-D` containing a dot**: `'-Dmaven.test.failure.ignore=true'`. Unquoted, the parser splits it and Maven reports "Unknown lifecycle phase".
- **Never use `-pl`.** Tycho derives the reactor from the OSGi manifests, so `-pl <module> -am` misses required bundles and fails with "Missing requirement". Build everything and narrow with `-Dtest=`.
- Use **`verify`, not `package`**: `jlink` runs at `pre-integration-test` and `archive-products` was moved to `verify`, so `package` leaves the product unarchived.

The runnable app is `de.tonsias.basis.product/target/products/tonsias/win32/win32/x86_64/Tonsias.exe`, next to the `jre` that `jlink` builds into it (the product declares no `-vm`, so without that directory the launcher dies silently). The module list is the `tonsias.jre.modules` property — Equinox derives the system bundle's packages from the image, so dropping a module removes packages (without `jdk.xml.dom` there is no `org.w3c.dom.css` and `org.eclipse.e4.ui.css.swt` fails to resolve). Only `win32.win32.x86_64` is built.

CI (`.github/workflows/build.yml`) runs the same `verify` on `windows-latest` with `-Dmaven.test.failure.ignore=true`, then fails the job from the totals that `.github/scripts/Summarize-TestResults.ps1` parses out of the surefire XML.

## Working in this repo

- **Target platform** `target-platform/target-platform.target` (Eclipse SDK 4.36, Guava, Gson, JUnit Jupiter) is the single source of truth for the IDE **and** the Tycho build — edit it rather than the poms. Set it active in the IDE before anything resolves. There is deliberately **no mocking framework**.
- **Run the app**: launch `de.tonsias.basis.product/tonsias.product`. `org.apache.felix.scr` and `org.eclipse.equinox.event` must be in the `autoStart` config or no service resolves; the Tycho test runtime configures the same two.
- **No `.launch` files are committed** — create them from the wizards.
- **Java compliance differs per bundle** and the BREE in `MANIFEST.MF` often disagrees with `.settings/org.eclipse.jdt.core.prefs` (`basis.ui` declares 24 / compiles at 19, `basis.logic` 24 / 22, `basis.osgi` and `basis.model` 19 / 19). A feature usable in one bundle may not compile in another. When adding a bundle, copy the settings from a sibling.

## Tests

Every test is a **system test**, in a `…test.system` package, run inside OSGi (Eclipse JUnit Plug-in Test, or `./mvnw verify`). One rule: **nothing is substituted.** The subject is the registered `@Component` from the service registry, the broker is the real e4 broker, files land in the real instance location (`target/work/data`, cleaned per run), and the SWT tests run on a real `Display`. Judge a call by the state it leaves and the events that left the bus — never by what a collaborator was told.

`de.tonsias.basis.osgi.test` exports its harness (`x-friends` to the logic and delta-view test bundles):

- **`ProductRuntime`** — start here in `@BeforeEach`. `start()` primes the runtime and returns the root instanz; accessors for every service; `flushDeltas()`; helpers that read objects back off disk.
- **`E4ServiceContext.prime()`** — what `start()` calls. `IEventBrokerBridge` and `IDeltaService` come from `IContextFunction`s that only run when a context is asked for their key, and `ChangePropagationListener` is an e4 addon in `Application.e4xmi`. The workbench triggers all three; headless nothing does, so without priming `InstanzServiceImpl`'s mandatory `@Reference IEventBrokerBridge` stays unsatisfied and the component never activates.
- **`EventRecorder`** — collects what passes on the bus, so a test can assert on a whole propagation chain. `awaitCount` / `awaitTopic` cover `Type.POST` and work running in a `Job`.

The runtime is **shared by every test in a bundle** — one Equinox, one root instanz, one key sequence, one delta log. Build your own subtree below the root instead of assuming an empty model, and call `ProductRuntime.flushDeltas()` in `@AfterEach`. Where a shared singleton makes a scenario unreachable (`KeyServiceImpl`'s counter), build a second **real** instance on its own preference node rather than a stand-in.

## Bundle layout and dependency direction

```
basis.model          POJOs, no OSGi deps (Guava BiMap only)
basis.data.access    Gson persistence: LoadService / SaveService / DeleteService
basis.osgi           services — contract and implementation, split by Export-Package
basis.logic          headless view logic (Eclipse Jobs), no SWT
basis.ui             e4 parts, handlers, dialogs  ── Application.e4xmi lives here
basis.ui.i18n        Messages class + OSGI-INF/l10n bundles
basis.icon           IconUtil + res/*.png
delta.view.*         the Delta view feature: logic / ui (fragment.e4xmi) / ui.test
```

`de.tonsias.basis.osgi.impl` (the `*ServiceImpl`s, context functions, `ChangePropagationListener`) is `x-friends` to the test bundle only. **UI and logic depend on `…osgi.intf` alone — never on `…osgi.impl`.**

## Core architecture

**Model.** `IInstanz` is the single tree node type: own key, parent key, child keys, and per-`SingleValueType` `BiMap<valueKey, name>` of attributes. Attributes are `ISingleValue` objects in their own files. **Everything is referenced by string key, never by object reference** — services resolve through a cache and fall back to disk. Key `"0"` is the root by convention. Keys are a base-36 counter (`KeyServiceImpl.KEYCHARS`), **lower case only** (file names on case-insensitive filesystems) and **sorted ascending** (binary search).

**Persistence.** One JSON file per object under `Platform.getInstanceLocation()`, at `ISavePathOwner.getPath()` + `getOwnKey()` + `.json`.

**Event flow — the heart of the app.** Everything model-changing goes through the bus, wrapped by `IEventBrokerBridge` (`Type.SEND` synchronous, `Type.POST` async). **Every mutating service method takes a `Type` and fires an event.**

- Topics and payload records live together in `…intf.non.service.*EventConstants`. **Adding a topic means adding its payload record next to it and registering it in `KNOWN_DELTA`.**
- `ChangePropagationListener` keeps both ends of every relation in sync and re-enters the services with `Type.SEND`, so **a careless new listener can loop**. The services guard by returning `false` without firing when already in the requested state — keep that.
- `IDeltaService` accumulates every delta since the last save. `saveDeltas()` folds the log into four key sets, calls the services, and resets. `OPEN_OPERATION`/`CLOSE_OPERATION` bracket an operation; `SAVE_ALL` triggers the save; the Delta view renders the log using those brackets.

**Dependency injection — three mechanisms coexist.**

1. **DS `@Component` + `@Reference`** for the plain services. Components are declared by **hand-maintained XML in `OSGI-INF/` referenced from `Service-Component:`** — adding one means both edits. (`de.tonsias.basis.osgi/META-INF/MANIFEST.MF` still lists two `…osgi.util.*.xml` files that do not exist.)
2. **`ContextFunction`** for services needing the e4 context (`EventBrokerContextFunction`, `DeltaServiceContextFunction`). Note they build a **new** instance per `compute(..)` — see issue #52.
3. **`OsgiUtil.lazyLoading(Class, Consumer)`** for code constructed before OSGi is ready (`ChangePropagationListener`).

**UI.** e4 model-first; parts are POJOs with `@PostConstruct postConstruct(Composite parent)` and `@Inject` fields. Non-trivial behaviour belongs in a `*.logic` bundle so it is testable without SWT — keep that split. Views react through `@UIEventTopic`/`@EventTopic` rather than polling.

**i18n — two levels, don't mix them.**

- **e4 model labels** (`%part.modelview`) resolve against `OSGI-INF/l10n/bundle.properties` / `bundle_de.properties` in the bundle owning the model file.
- **Java strings** use `@Inject @Translation Messages` — adding one means a field **and** the key in every locale file. `de.tonsias.basis.ui.i18n` is the only `Messages` bundle.

Keep the German files in `\uXXXX` escapes (`Properties.load` reads ISO-8859-1). `TranslationCoverageTest` and `FragmentTranslationCoverageTest` fail the build on a missing key. Not everything textual is UI text: `Job`/`JobGroup` names are diagnostic and never displayed (no trim bar), and `IObject.toString()` and preference *node* paths are identifiers — only preference *keys* go through `MessagesUtil.getPreferenceLabel`.

## Conventions

- Fields prefixed `_` (`_instanzService`), record components too (`_parentKey`). Constants `UPPER_SNAKE`.
- `I*` for OSGi service interfaces — except `data.access`, which is unprefixed (`LoadService`). Abstract bases are `A*`.
- Adding a bundle needs three edits beyond the project: `Require-Bundle` in the consumers, a `<plugin>` entry in the owning `feature.xml`, and `x-friends` for packages consumed only by tests.

## Workflow

Every change follows this sequence, and **never commit directly to `main`**:

1. Switch to `main` and pull.
2. Branch off it (`feat-<name>`).
3. Commit there (`feat <name>: …` / `fix <name>: …`).
4. Run `.\build.ps1` and fix the failures. The suite is **green on `main`**, so a failure is yours. Never weaken an assertion to make one pass — check the test against the production code first.
5. Write tests for the new code: `…test.system` package of the bundle matching the layer, starting from `ProductRuntime`. A new bundle's internals need `x-friends` before its test bundle can see them.
6. Push and open a PR into `main`.

**Open a GitHub issue for anything you find along the way** — a bug, a latent fragility, a wrong comment, dead configuration — whenever it is outside the scope of what you were asked to do. Do not silently fix it, and do not only mention it in the PR description. Use `gh issue create` (German title, `bug` label where it applies), say what happens, why it matters, how it surfaced, and a possible approach. Then reference it from the code or test that runs into it, so the next reader lands on the explanation. Fixing it in place is only right when the task cannot be completed otherwise — and then it belongs in the PR description.

## Releasing

Latest release **0.1.0**; the reactor is on **0.2.0-SNAPSHOT**. `CHANGELOG.md` is the per-release record, `README.md` the user-facing overview. Versions live in 37 files — **never edit them by hand**:

```powershell
$env:JAVA_HOME = '<jdk24>'
.\mvnw.cmd org.eclipse.tycho:tycho-versions-plugin:5.0.3:set-version "-DnewVersion=<x.y.z>" "-DupdateVersionRangeMatchingBounds=true"
```

It skips the `version` attribute of `tonsias.product` when that attribute does not already match the old version — check it afterwards. Then build, add a `CHANGELOG.md` section, and follow the workflow above. The tag is `v<x.y.z>` on the merge commit, with the product zip and p2 repository attached to the GitHub release.
