[CmdletBinding()]
param(
    [string]$SourceDesign,
    [string]$TargetDesign,
    [switch]$Apply,
    [switch]$All,
    [switch]$ChangedOnly,
    [switch]$IncludeProjectSnapshot,
    [switch]$IncludeProjectDocs,
    [string[]]$Include,
    [string[]]$Exclude = @()
)

$ErrorActionPreference = "Stop"

if (-not $SourceDesign) {
    $SourceDesign = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
}
if (-not $TargetDesign) {
    throw "TargetDesign is required. Example: -TargetDesign D:\workspace\FastFilm\dev\design"
}

function Resolve-FullPath {
    param([string]$Path)
    if ([System.IO.Path]::IsPathRooted($Path)) {
        return [System.IO.Path]::GetFullPath($Path)
    }
    return [System.IO.Path]::GetFullPath((Join-Path (Get-Location).Path $Path))
}

function Normalize-RelativePath {
    param([string]$Path)
    return $Path.Trim().TrimStart("\", "/").Replace("/", "\")
}

function Get-RelativeDesignPath {
    param(
        [string]$Root,
        [string]$Path
    )
    $fullRoot = Resolve-FullPath $Root
    $fullPath = Resolve-FullPath $Path
    if ($fullPath.StartsWith($fullRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        return Normalize-RelativePath $fullPath.Substring($fullRoot.Length)
    }
    return Normalize-RelativePath $Path
}

function Get-FileHashText {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        return $null
    }
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
}

function Invoke-GitOutput {
    param(
        [string]$RepoRoot,
        [string[]]$Arguments
    )

    $oldErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        return (& git -C $RepoRoot @Arguments 2>&1) |
            Where-Object { $_ -is [string] -and $_ -notmatch "^warning:" }
    } finally {
        $ErrorActionPreference = $oldErrorActionPreference
    }
}

function Test-ShouldSkip {
    param([string]$RelativePath)

    $normalized = Normalize-RelativePath $RelativePath
    if (-not $IncludeProjectSnapshot -and $normalized -ieq "code-interface-scan-summary.md") {
        return $true
    }
    if (-not $IncludeProjectDocs -and ($normalized -ieq "project" -or $normalized -ilike "project\*")) {
        return $true
    }

    foreach ($pattern in $Exclude) {
        $normalizedPattern = Normalize-RelativePath $pattern
        if ($normalized -like $normalizedPattern) {
            return $true
        }
    }

    return $false
}

function Get-DefaultFiles {
    return @(
        "README.md",
        "android-check-quick.md",
        "android-codegen-quick-rules.md",
        "android-codegen-scenarios.md",
        "code-interface-directory.md",
        "scripts\README.md",
        "scripts\check_android_xml_tokens.ps1",
        "scripts\scan_code_interface.ps1",
        "scripts\sync_design_docs.ps1",
        "scripts\sync_design_to_fastfilm.ps1"
    )
}

function Get-AllFiles {
    param([string]$SourceRoot)

    Get-ChildItem -LiteralPath $SourceRoot -Recurse -File |
        ForEach-Object { Get-RelativeDesignPath $SourceRoot $_.FullName }
}

function Get-ChangedFiles {
    param([string]$SourceRoot)

    $repoRoot = Split-Path -Parent $SourceRoot
    $lines = Invoke-GitOutput $repoRoot @("status", "--porcelain", "--", "design")
    $result = New-Object System.Collections.Generic.List[string]

    foreach ($line in $lines) {
        if ($line.Length -lt 4) {
            continue
        }

        $status = $line.Substring(0, 2)
        if ($status -match "D") {
            continue
        }

        $path = $line.Substring(3).Trim()
        if ($path -match " -> ") {
            $path = ($path -split " -> ")[-1].Trim()
        }
        $path = $path.Trim('"')

        if ($path -notmatch "^(design/|design\\)") {
            continue
        }

        $relative = Normalize-RelativePath ($path -replace "^(design/|design\\)", "")
        $sourceFile = Join-Path $SourceRoot $relative
        if (Test-Path -LiteralPath $sourceFile) {
            $result.Add($relative) | Out-Null
        }
    }

    return $result
}

$sourceRoot = Resolve-FullPath $SourceDesign
$targetRoot = Resolve-FullPath $TargetDesign

if (-not (Test-Path -LiteralPath $sourceRoot)) {
    throw "Source design directory does not exist: $sourceRoot"
}
if ((Split-Path -Leaf $sourceRoot) -ne "design") {
    throw "SourceDesign must point to a design directory: $sourceRoot"
}
if ((Split-Path -Leaf $targetRoot) -ne "design") {
    throw "TargetDesign must point to a design directory: $targetRoot"
}
if (-not (Test-Path -LiteralPath $targetRoot)) {
    if ($Apply) {
        New-Item -ItemType Directory -Path $targetRoot | Out-Null
    } else {
        throw "Target design directory does not exist: $targetRoot"
    }
}

$selectedFiles = if ($Include -and $Include.Count -gt 0) {
    $Include | ForEach-Object { Normalize-RelativePath $_ }
} elseif ($ChangedOnly) {
    Get-ChangedFiles $sourceRoot
} elseif ($All) {
    Get-AllFiles $sourceRoot
} else {
    Get-DefaultFiles
}

$selectedFiles = @(
    $selectedFiles |
        ForEach-Object { Normalize-RelativePath $_ } |
        Where-Object { $_ -and -not (Test-ShouldSkip $_) } |
        Sort-Object -Unique
)

$copied = 0
$same = 0
$missing = 0
$different = 0

Write-Host "Design docs sync"
Write-Host "Source: $sourceRoot"
Write-Host "Target: $targetRoot"
Write-Host "Mode: $(if ($Apply) { 'apply' } else { 'dry-run' })"
Write-Host "Selection: $(if ($Include) { 'include-list' } elseif ($ChangedOnly) { 'changed-only' } elseif ($All) { 'all' } else { 'default-common' })"
Write-Host "Include project snapshot: $IncludeProjectSnapshot"
Write-Host "Include project docs: $IncludeProjectDocs"
Write-Host ""

foreach ($relative in $selectedFiles) {
    $sourceFile = Join-Path $sourceRoot $relative
    $targetFile = Join-Path $targetRoot $relative

    if (-not (Test-Path -LiteralPath $sourceFile)) {
        $missing++
        Write-Host "[missing-source] $relative"
        continue
    }

    $sourceHash = Get-FileHashText $sourceFile
    $targetHash = Get-FileHashText $targetFile
    if ($sourceHash -and $targetHash -and $sourceHash -eq $targetHash) {
        $same++
        Write-Host "[same] $relative"
        continue
    }

    $status = if ($targetHash) { "update" } else { "create" }
    $different++
    Write-Host ("[{0}] {1}" -f $status, $relative)

    if ($Apply) {
        $targetDir = Split-Path -Parent $targetFile
        if (-not (Test-Path -LiteralPath $targetDir)) {
            New-Item -ItemType Directory -Path $targetDir | Out-Null
        }
        Copy-Item -LiteralPath $sourceFile -Destination $targetFile -Force
        $copied++
    }
}

Write-Host ""
Write-Host "Summary:"
Write-Host "- selected: $($selectedFiles.Count)"
Write-Host "- same: $same"
Write-Host "- different or missing target: $different"
Write-Host "- missing source: $missing"
Write-Host "- copied: $copied"

if (-not $Apply) {
    Write-Host ""
    Write-Host "Dry run only. Re-run with -Apply to copy files."
}
