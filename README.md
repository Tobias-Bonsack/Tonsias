# Tonsias

[![Build](https://github.com/Tobias-Bonsack/Tonsias/actions/workflows/build.yml/badge.svg)](https://github.com/Tobias-Bonsack/Tonsias/actions/workflows/build.yml)

Tonsias is an Eclipse RCP / e4 desktop application for modelling data as a tree.
Every node is an _Instanz_; every Instanz carries named _single values_ (string or
integer) as its attributes. Nothing is stored in one big document — each Instanz and
each value is its own small JSON file, and every change travels through an event bus
before it is written, so the whole model stays observable and every edit is visible
and revocable until it is saved.

> **Version 0.1.0 is the first release.** It is an early, functional snapshot: the
> model, the persistence, the event chain and three views work end to end, but the
> feature set is deliberately small and only a Windows x86_64 build is produced.

## Features in 0.1.0

### Views

- **Model View** — the whole model as a tree of Instanzen and their values.
  Its context menu (also reachable with `Ctrl+N`) creates a child Instanz, adds a
  string or an integer single value through a dialog, or deletes a value. The tree
  refreshes itself from model events instead of polling, and which value is used as a
  node's label is a preference.
- **Property View** — everything about the selected Instanz: own key, parent key,
  child keys, and all single values grouped by type. Value and value name are edited
  in place; edited fields turn green, fields marked for deletion turn red, and the
  part is flagged dirty. `Ctrl+S` writes the pending edits, `F5` re-reads the view,
  and switching to another Instanz while dirty asks first whether to keep the changes.
- **Delta View** — every change since the last save, as a tree grouped per operation.
  Its toolbar refreshes the tree and saves everything in one go.

### Editing and saving

- Edits are collected, not written through: `IDeltaService` accumulates every model
  event and `Save all` folds that log into four key sets — Instanzen to save, Instanzen
  to delete, values to save, values to delete — then calls the services once per set.
- Saving is available from `File > Save`, from the window toolbar and with `Ctrl+S`.
- Edits in the Property View are debounced through Eclipse `Job`s in a serial job
  group per value key, so holding down a key does not queue one write per character.
- Both sides of every relation are kept consistent automatically: adding a child to an
  Instanz sets the parent on the other object, and vice versa.

### Persistence

- One JSON file per object, written with Gson under the Eclipse instance location:
  `instanz/<key>.json`, `single_value/string/<key>.json`, `single_value/integer/<key>.json`.
- Objects are referenced by string key, never by object reference; the services cache
  what they have loaded and only touch the disk on a miss or a save.
- Keys come from a base-36 counter over a lower-case alphabet, persisted in the Eclipse
  instance preferences. Key `0` is the root Instanz.

### Preferences and i18n

- `Window > Preferences` shows the key service and the basic preferences: which value
  name is displayed in the Model View, whether values are shown in the tree, and the
  model root path (read-only, click to copy).
- Both the e4 model labels and the Java strings are translated; **English** and
  **German** ship with this release.

### Build and quality

- A headless **Tycho** build mirrors the IDE: `.\build.ps1` compiles every bundle, runs
  every test bundle inside a real Equinox, and materialises the runnable product.
- The test bundles cover the model, the JSON persistence, the services, the view logic
  and the complete event chain. All of them run inside OSGi.
- GitHub Actions builds every push and pull request on Windows and posts the per-bundle
  test results as a single, edited-in-place pull request comment.

## Download and run

1. Take `tonsias-win32.win32.x86_64.zip` from the
   [0.1.0 release](https://github.com/Tobias-Bonsack/Tonsias/releases/tag/v0.1.0).
2. Unzip it anywhere and start `Tonsias.exe`.

From 0.2.0 on the zip is self-contained: it carries its own Java runtime in `jre/`
next to the launcher, so **nothing has to be installed** and the launcher finds a VM
without a `-vm` argument. The 0.1.0 zip does not — it needs Java 24 or newer on the
`PATH`, or an explicit `Tonsias.exe -vm <jdk>\bin\javaw.exe`, and starts silently into
nothing without either.

The model is written to the application's instance location — the path is shown in
`Window > Preferences` and can be pointed elsewhere with `-data <directory>`.

`de.tonsias.basis.product-0.1.0.zip` in the same release is the p2 repository, for
installing or updating Tonsias from within Eclipse instead.

## Build from source

```powershell
.\build.ps1                                # compile, test, materialize the product
.\build.ps1 -SkipTests                     # product only
.\build.ps1 -- -Dtest=KeyServiceImplTest   # everything after -- goes to Maven verbatim
```

`build.ps1` exists because `JAVA_HOME` is usually not set on a development machine: it
locates a JDK 24, exports it and then runs the Maven wrapper. With a JDK 24 in
`JAVA_HOME` the wrapper works directly, too:

```bash
./mvnw clean verify
```

Do not narrow the reactor with `-pl`. Tycho derives the reactor from the OSGi manifests
rather than from Maven dependencies, so a partial reactor fails to resolve; narrow the
test run with `-Dtest=` instead.

The build produces, under `de.tonsias.basis.product/target`:

| Path                                              | What it is                       |
| ------------------------------------------------- | -------------------------------- |
| `products/tonsias/win32/win32/x86_64/Tonsias.exe` | the launcher — run this          |
| `products/tonsias/win32/win32/x86_64/jre`         | the bundled Java runtime         |
| `products/tonsias-win32.win32.x86_64.zip`         | the distributable                |
| `de.tonsias.basis.product-<version>.zip`          | the p2 repository                |

`jre` is built by `jlink` from the JDK that runs the build and makes the installation
independent of the machine it lands on — the launcher searches for a VM there when no
`-vm` is given. It is what makes the zip 76 MB rather than 8. Because the runtime has to
be in place before the zip is written, `archive-products` runs at `verify` rather than at
`package`; `mvn package` alone therefore leaves the product unarchived.

Only `win32/win32/x86_64` is built; add `<environment>`s to
`target-platform-configuration` in the parent `pom.xml` for other platforms — each needs
its own `jlink` run, from a JDK for that platform.

## Developing in the Eclipse IDE

Tonsias is an Eclipse PDE workspace. Import all projects, then set
`target-platform/target-platform.target` as the active target platform — it is the
single source of truth for the IDE **and** for the Tycho build (Eclipse SDK 4.36 plus
Guava, Gson and JUnit Jupiter from Maven Central). Nothing resolves before it is
active.

Launch `de.tonsias.basis.product/tonsias.product` to run the application, and run the
test bundles as _Eclipse JUnit Plug-in Tests_. Launch configurations are deliberately
not committed; create them from the wizards.

The suite is made up of system tests only: each one drives the services the running
application registers, on the real event bus and the real workspace files, with no
mocking framework anywhere in the build.

## Repository layout

| Bundle                        | Contents                                                            |
| ----------------------------- | ------------------------------------------------------------------- |
| `de.tonsias.basis.model`      | the POJO model: `IInstanz`, `ISingleValue`, `SingleValueType`        |
| `de.tonsias.basis.data.access`| Gson persistence: load, save and delete services                    |
| `de.tonsias.basis.osgi`       | service interfaces (`…osgi.intf`) and their implementations         |
| `de.tonsias.basis.logic`      | headless view logic, no SWT — unit-testable                         |
| `de.tonsias.basis.ui`         | the e4 parts, handlers and dialogs; owns `Application.e4xmi`         |
| `de.tonsias.basis.ui.i18n`    | `Messages` and the translated properties                            |
| `de.tonsias.basis.icon`       | icons and `IconUtil`                                                |
| `de.tonsias.delta.view.*`     | the Delta view: logic, UI (as an e4 model fragment), i18n, tests     |
| `de.tonsias.basis.feature`    | the features assembled into `de.tonsias.basis.product`              |
| `target-platform`             | the target definition shared by the IDE and the build               |

UI and logic code depends on `de.tonsias.basis.osgi.intf` only; the `…osgi.impl`
package is exported to the test bundle alone.

## Documentation

- [`CHANGELOG.md`](CHANGELOG.md) — what is in each release.
- [`CLAUDE.md`](CLAUDE.md) — the architecture in depth: event flow, the three dependency
  injection mechanisms that coexist, persistence layout, conventions, and the rules a
  change has to follow.

## Known limitations in 0.1.0

- Only a Windows x86_64 product is built.
- Single values are limited to string and integer.
- Instanzen can be created but not deleted from the user interface; values can.
- The Java compliance levels of the bundles are inconsistent (19 / 22 / 24), which is
  why the Tycho build has to pin its resolution execution environment.

## License

[MIT](LICENSE) © 2025 Tobias-Bonsack

That covers the `de.tonsias.*` bundles. The built product is an aggregate: the zip under
`de.tonsias.basis.product/target` also contains the Eclipse Platform bundles, which are
[EPL-2.0](https://www.eclipse.org/legal/epl-2.0/) and stay under their own terms — each
one carries its `about.html` inside its jar. Guava and Gson are Apache-2.0.
