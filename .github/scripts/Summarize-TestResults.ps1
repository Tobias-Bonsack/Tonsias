<#
.SYNOPSIS
    Folds the surefire reports of every test bundle into one markdown report.

.DESCRIPTION
    Reads all <repo>/*/target/surefire-reports/TEST-*.xml, groups them by the
    bundle that produced them and writes a markdown summary: one table row per
    test bundle plus a detail section for every failed or errored test case.

    Written for the PR comment in .github/workflows/tests.yml, but runnable
    locally after a build to see the same report:

        .\build.ps1
        .\.github\scripts\Summarize-TestResults.ps1

.PARAMETER RepoRoot
    Root of the reactor. Defaults to the repository this script lives in.

.PARAMETER OutFile
    Where to write the markdown. Defaults to test-report.md under RepoRoot.

.PARAMETER Title
    Heading of the report.

.PARAMETER MaxFailureDetails
    How many failed test cases to spell out in full before only counting the
    rest - a PR comment is capped at 65536 characters.
#>
[CmdletBinding()]
param(
    [string] $RepoRoot = (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)),
    [string] $OutFile,
    [string] $Title = 'Test results',
    [int] $MaxFailureDetails = 20
)

$ErrorActionPreference = 'Stop'
# Default into the reactor's own target directory, which .gitignore already
# covers, so a local run leaves nothing untracked behind.
if (-not $OutFile) {
    $targetDir = Join-Path $RepoRoot 'target'
    if (-not (Test-Path $targetDir)) { New-Item -ItemType Directory -Path $targetDir | Out-Null }
    $OutFile = Join-Path $targetDir 'test-report.md'
}

# Tycho surefire writes one TEST-<class>.xml per test class below the test
# bundle's target directory. Globbing the whole reactor finds every test bundle
# without naming any of them here, so a new one shows up by itself. @() keeps
# .Count meaningful when nothing matches.
$reportFiles = @(Get-ChildItem -Path $RepoRoot -Recurse -Filter 'TEST-*.xml' -ErrorAction SilentlyContinue |
        Where-Object { $_.DirectoryName -like '*\target\surefire-reports' })

$bundles = New-Object System.Collections.Generic.List[object]
$failureDetails = New-Object System.Collections.Generic.List[object]
$totals = [ordered]@{ tests = 0; failures = 0; errors = 0; skipped = 0; time = 0.0 }

# Group by the bundle directory, i.e. the path segment above "target".
$byBundle = $reportFiles | Group-Object { Split-Path -Leaf (Split-Path -Parent (Split-Path -Parent $_.DirectoryName)) }

foreach ($group in ($byBundle | Sort-Object Name)) {
    $bundle = [ordered]@{ name = $group.Name; tests = 0; failures = 0; errors = 0; skipped = 0; time = 0.0 }

    foreach ($file in $group.Group) {
        [xml] $xml = Get-Content -Path $file.FullName -Raw
        $suite = $xml.testsuite
        if (-not $suite) { continue }

        $bundle.tests += [int] $suite.tests
        $bundle.failures += [int] $suite.failures
        $bundle.errors += [int] $suite.errors
        $bundle.skipped += [int] $suite.skipped
        $bundle.time += [double]::Parse($suite.time, [Globalization.CultureInfo]::InvariantCulture)

        foreach ($case in $suite.testcase) {
            foreach ($kind in @('failure', 'error')) {
                $problem = $case.$kind
                if (-not $problem) { continue }
                # The <failure> element carries the message as an attribute and
                # the stack trace as its text; a bare element has neither.
                $message = $problem.message
                if (-not $message) { $message = $problem.type }
                if (-not $message) { $message = '(no message)' }
                $stack = ''
                if ($problem -is [string]) { $stack = $problem } elseif ($problem.'#text') { $stack = $problem.'#text' }

                $failureDetails.Add([pscustomobject]@{
                        Bundle  = $group.Name
                        Class   = $case.classname
                        Test    = $case.name
                        Kind    = $kind
                        Message = $message
                        Stack   = $stack
                    })
            }
        }
    }

    foreach ($key in @('tests', 'failures', 'errors', 'skipped', 'time')) { $totals[$key] += $bundle[$key] }
    $bundles.Add([pscustomobject] $bundle)
}

# --- markdown --------------------------------------------------------------
# Format durations invariantly: on a German dev machine "{0:N1}" would render
# 3.6 as "3,6".
function Format-Seconds([double] $seconds) {
    return [string]::Format([Globalization.CultureInfo]::InvariantCulture, '{0:N1}', $seconds)
}

$broken = $totals.failures + $totals.errors
$lines = New-Object System.Collections.Generic.List[string]

if ($reportFiles.Count -eq 0) {
    $verdict = ':warning: **No test reports found** - the build very likely failed before the tests ran.'
}
elseif ($broken -eq 0) {
    $verdict = ":white_check_mark: **All $($totals.tests) tests passed** in $($bundles.Count) test bundles."
}
else {
    $verdict = ":x: **$broken of $($totals.tests) tests failed** ($($totals.failures) failures, $($totals.errors) errors)."
}

$lines.Add("### $Title")
$lines.Add('')
$lines.Add($verdict)
$lines.Add('')

if ($reportFiles.Count -gt 0) {
    $lines.Add('| Test bundle | Tests | Failures | Errors | Skipped | Time |')
    $lines.Add('| --- | ---: | ---: | ---: | ---: | ---: |')
    foreach ($bundle in $bundles) {
        $mark = ':white_check_mark:'
        if (($bundle.failures + $bundle.errors) -gt 0) { $mark = ':x:' }
        $lines.Add("| $mark ``$($bundle.name)`` | $($bundle.tests) | $($bundle.failures) | $($bundle.errors) | $($bundle.skipped) | $(Format-Seconds $bundle.time)s |")
    }
    $lines.Add("| **Total** | **$($totals.tests)** | **$($totals.failures)** | **$($totals.errors)** | **$($totals.skipped)** | **$(Format-Seconds $totals.time)s** |")
    $lines.Add('')
}

if ($failureDetails.Count -gt 0) {
    $lines.Add('<details><summary>Failed tests</summary>')
    $lines.Add('')
    $shown = 0
    foreach ($failure in $failureDetails) {
        if ($shown -ge $MaxFailureDetails) { break }
        $shown++
        $simpleClass = ($failure.Class -split '\.')[-1]
        $lines.Add("**$simpleClass.$($failure.Test)** - $($failure.Kind) in ``$($failure.Bundle)``")
        $lines.Add('')
        $lines.Add('```')
        $lines.Add($failure.Message.Trim())
        # A couple of stack frames are usually enough to locate the assertion;
        # the full trace is in the uploaded surefire-reports artifact.
        $frames = ($failure.Stack -split "`n" | Where-Object { $_.Trim().StartsWith('at ') } | Select-Object -First 4)
        foreach ($frame in $frames) { $lines.Add($frame.TrimEnd()) }
        $lines.Add('```')
        $lines.Add('')
    }
    if ($failureDetails.Count -gt $shown) {
        $lines.Add("_... and $($failureDetails.Count - $shown) more - see the ``surefire-reports`` artifact._")
        $lines.Add('')
    }
    $lines.Add('</details>')
    $lines.Add('')
}

$markdown = ($lines -join "`n")
Set-Content -Path $OutFile -Value $markdown -Encoding utf8
Write-Host $markdown

# --- outputs ---------------------------------------------------------------
if ($env:GITHUB_OUTPUT) {
    $failedFlag = 'false'
    if ($broken -gt 0 -or $reportFiles.Count -eq 0) { $failedFlag = 'true' }
    Add-Content -Path $env:GITHUB_OUTPUT -Encoding utf8 -Value @(
        "failed=$failedFlag",
        "tests=$($totals.tests)",
        "failures=$($totals.failures)",
        "errors=$($totals.errors)",
        "skipped=$($totals.skipped)",
        "report-file=$OutFile"
    )
}
if ($env:GITHUB_STEP_SUMMARY) { Add-Content -Path $env:GITHUB_STEP_SUMMARY -Encoding utf8 -Value $markdown }
