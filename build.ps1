<#
.SYNOPSIS
    Builds Tonsias with Tycho and materializes the runnable product.

.DESCRIPTION
    Locates a JDK 24 (the highest BREE in the reactor), exports it as JAVA_HOME
    for the Maven wrapper and runs the whole reactor. Everything after the
    script's own parameters is passed on to Maven verbatim.

.PARAMETER SkipTests
    Compile and assemble only (-DskipTests).

.PARAMETER Goals
    Maven lifecycle phases to run. Defaults to "clean verify".

.EXAMPLE
    .\build.ps1
    .\build.ps1 -SkipTests
    .\build.ps1 -- -Dtest=KeyServiceImplTest
#>
[CmdletBinding()]
param(
    [switch] $SkipTests,
    [string[]] $Goals = @('clean', 'verify'),
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $MavenArgs
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

# --- JDK 24 ----------------------------------------------------------------
# The bundles' highest BREE is JavaSE-24 and target-platform-configuration pins
# the resolution EE to it, so anything older fails to resolve Eclipse 4.36.
# Read the version from the JDK's own "release" file rather than invoking
# java -version: PowerShell 5.1 turns a native command's stderr into an
# ErrorRecord, which $ErrorActionPreference = 'Stop' would make fatal.
function Get-JdkVersion([string] $javaHome) {
    if (-not (Test-Path (Join-Path $javaHome 'bin\java.exe'))) { return $null }
    $releaseFile = Join-Path $javaHome 'release'
    if (-not (Test-Path $releaseFile)) { return $null }
    $line = Get-Content $releaseFile | Where-Object { $_ -match '^JAVA_VERSION="(\d+)' }
    if ($line) { return [int]$Matches[1] }
    return $null
}

function Find-Jdk24 {
    $candidates = New-Object System.Collections.Generic.List[string]
    if ($env:JAVA_HOME) { $candidates.Add($env:JAVA_HOME) }

    $searchRoots = @(
        'C:\dev\java',
        "$env:ProgramFiles\Java",
        "$env:ProgramFiles\Eclipse Adoptium",
        "$env:ProgramFiles\Microsoft\jdk",
        "$env:LOCALAPPDATA\Programs\Eclipse Adoptium"
    )
    foreach ($root in $searchRoots) {
        if (-not (Test-Path $root)) { continue }
        Get-ChildItem -Path $root -Directory -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending |
            ForEach-Object { $candidates.Add($_.FullName) }
    }

    foreach ($candidate in $candidates) {
        if ((Get-JdkVersion $candidate) -ge 24) { return $candidate }
    }
    return $null
}

$jdk = Find-Jdk24
if (-not $jdk) {
    throw "No JDK 24 or newer found. Set JAVA_HOME to one, or install it under one of: C:\dev\java, $env:ProgramFiles\Java."
}
$env:JAVA_HOME = $jdk
Write-Host "JAVA_HOME = $jdk"

# --- build -----------------------------------------------------------------
# Never narrow the reactor with -pl: Tycho derives it from the OSGi manifests,
# not from Maven dependencies, so a partial reactor fails on "Missing
# requirement". Narrow the test run with -Dtest= instead.
$arguments = @($Goals)
if ($SkipTests) { $arguments += '-DskipTests' }
if ($MavenArgs) { $arguments += ($MavenArgs | Where-Object { $_ -ne '--' }) }

Write-Host "mvnw $($arguments -join ' ')"
# Maven writes progress to stderr. When the caller redirects the script's
# streams (".\build.ps1 2>&1 | Tee-Object build.log"), Windows PowerShell wraps
# every such line in a NativeCommandError, which 'Stop' would make fatal
# regardless of the exit code. Judge the build by $LASTEXITCODE instead.
$previousPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try {
    & (Join-Path $repoRoot 'mvnw.cmd') @arguments
} finally {
    $ErrorActionPreference = $previousPreference
}
if ($LASTEXITCODE -ne 0) {
    throw "Maven failed with exit code $LASTEXITCODE."
}

# --- report ----------------------------------------------------------------
$productDir = Join-Path $repoRoot 'de.tonsias.basis.product\target\products\tonsias\win32\win32\x86_64'
$launcher = Join-Path $productDir 'Tonsias.exe'
$archives = Get-ChildItem -Path (Join-Path $repoRoot 'de.tonsias.basis.product\target\products') -Filter '*.zip' -ErrorAction SilentlyContinue

Write-Host ''
Write-Host 'BUILD OK'
if (Test-Path $launcher) { Write-Host "  product   $launcher" }
foreach ($archive in $archives) { Write-Host "  archive   $($archive.FullName)" }
