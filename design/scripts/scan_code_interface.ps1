[CmdletBinding()]
param(
    [string]$SourceRoot = "app/src/main/java",
    [string]$ResRoot = "app/src/main/res",
    [string]$OutputPath,
    [switch]$WriteSummary,
    [string]$ComparePath,
    [int]$MaxDiffLines = 160,
    [switch]$FailOnDiff
)

$ErrorActionPreference = "Stop"

$RepoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path

function Resolve-RepoPath {
    param([string]$InputPath)
    if ([System.IO.Path]::IsPathRooted($InputPath)) {
        return $InputPath
    }
    return (Join-Path $RepoRoot $InputPath)
}

function Get-RelativePath {
    param([string]$InputPath)
    $fullPath = (Resolve-Path -LiteralPath $InputPath).Path
    if ($fullPath.StartsWith($RepoRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        return $fullPath.Substring($RepoRoot.Length).TrimStart("\", "/")
    }
    return $fullPath
}

function Read-TextLines {
    param([string]$File)
    if (-not (Test-Path -LiteralPath $File)) {
        return @()
    }
    try {
        return Get-Content -LiteralPath $File -Encoding UTF8
    } catch {
        return Get-Content -LiteralPath $File
    }
}

function Get-NormalizedSummaryLines {
    param([string[]]$Lines)

    return $Lines |
        Where-Object { $_ -notmatch "^Generated:\s" } |
        ForEach-Object { $_.TrimEnd() }
}

function Get-SourceFiles {
    $root = Resolve-RepoPath $SourceRoot
    if (-not (Test-Path -LiteralPath $root)) {
        throw "SourceRoot does not exist: $SourceRoot"
    }

    Push-Location $RepoRoot
    try {
        $rgFiles = rg --files $SourceRoot -g "*.kt" -g "*.java" -g "!**/OssManager.kt" 2>$null
        if ($LASTEXITCODE -eq 0 -or $rgFiles.Count -gt 0) {
            return $rgFiles | ForEach-Object { Resolve-RepoPath $_ }
        }
    } finally {
        Pop-Location
    }

    return Get-ChildItem -LiteralPath $root -Recurse -File -Include "*.kt", "*.java" |
        Where-Object { $_.Name -ne "OssManager.kt" } |
        ForEach-Object { $_.FullName }
}

function Get-FirstRegexValue {
    param(
        [string[]]$Lines,
        [string]$Pattern
    )
    foreach ($line in $Lines) {
        if ($line -match $Pattern) {
            return $Matches[1]
        }
    }
    return $null
}

function Get-ManifestApplicationName {
    param([string[]]$Lines)

    for ($i = 0; $i -lt $Lines.Count; $i++) {
        if ($Lines[$i] -notmatch "<application\b") {
            continue
        }

        $block = $Lines[$i]
        $cursor = $i
        while ($block -notmatch ">" -and $cursor + 1 -lt $Lines.Count) {
            $cursor++
            $block += " " + $Lines[$cursor]
        }

        if ($block -match "android:name\s*=\s*['`"]([^'`"]+)['`"]") {
            return $Matches[1]
        }
        return $null
    }

    return $null
}

$sourceFiles = @(Get-SourceFiles | Sort-Object -Unique)
$kotlinCount = @($sourceFiles | Where-Object { $_ -match "\.kt$" }).Count
$javaCount = @($sourceFiles | Where-Object { $_ -match "\.java$" }).Count

$settingsFile = Resolve-RepoPath "settings.gradle"
$appGradle = Resolve-RepoPath "app/build.gradle"
$configGradle = Resolve-RepoPath "config.gradle"
$manifestFile = Resolve-RepoPath "app/src/main/AndroidManifest.xml"
$apiServiceFile = Resolve-RepoPath "app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/ApiService.kt"
$apiConstantsFile = Resolve-RepoPath "app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/ApiConstants.kt"

$settingsLines = Read-TextLines $settingsFile
$gradleLines = Read-TextLines $appGradle
$configLines = Read-TextLines $configGradle
$manifestLines = Read-TextLines $manifestFile

$namespace = Get-FirstRegexValue $gradleLines "^\s*namespace\s+['`"]([^'`"]+)['`"]"
$applicationId = Get-FirstRegexValue $gradleLines "^\s*applicationId\s+['`"]([^'`"]+)['`"]"
if (-not $applicationId) {
    $applicationId = Get-FirstRegexValue $gradleLines "^\s*applicationId\s+(.+)$"
}
if ($applicationId -eq "rootProject.ext.android.applicationId") {
    $resolvedApplicationId = Get-FirstRegexValue $configLines "applicationId\s*:\s*['`"]([^'`"]+)['`"]"
    if ($resolvedApplicationId) {
        $applicationId = $resolvedApplicationId
    }
}
$applicationName = Get-ManifestApplicationName $manifestLines

$modules = New-Object System.Collections.Generic.List[string]
foreach ($line in $settingsLines) {
    if ($line -match "include\s+(.+)") {
        $Matches[1] -split "," |
            ForEach-Object { $_.Trim().Trim("'", '"') } |
            Where-Object { $_ } |
            ForEach-Object { $modules.Add($_) | Out-Null }
    }
}

$classRows = New-Object System.Collections.Generic.List[object]
foreach ($file in $sourceFiles) {
    $relative = Get-RelativePath $file
    $lines = Read-TextLines $file
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        if ($line -match "\b(class|object|interface)\s+(\w+(Activity|Fragment|ViewModel|Repository|Adapter|Dialog))\b") {
            $classRows.Add([pscustomobject]@{
                Name = $Matches[2]
                Kind = $Matches[3]
                File = $relative
                Line = $i + 1
            }) | Out-Null
        }
    }
}

$apiMethods = New-Object System.Collections.Generic.List[object]
if (Test-Path -LiteralPath $apiServiceFile) {
    $apiLines = Read-TextLines $apiServiceFile
    $lastAnnotation = ""
    for ($i = 0; $i -lt $apiLines.Count; $i++) {
        $line = $apiLines[$i].Trim()
        if ($line -match "^@(GET|POST|PUT|DELETE|PATCH)\((.+)\)") {
            $lastAnnotation = $line
        }
        if ($line -match "\bsuspend\s+fun\s+(\w+)\s*\(") {
            $apiMethods.Add([pscustomobject]@{
                Name = $Matches[1]
                Annotation = $lastAnnotation
                Line = $i + 1
            }) | Out-Null
            $lastAnnotation = ""
        }
    }
}

$repositories = @($classRows | Where-Object { $_.Kind -eq "Repository" } | Sort-Object Name)
$viewModels = @($classRows | Where-Object { $_.Kind -eq "ViewModel" } | Sort-Object Name)
$activities = @($classRows | Where-Object { $_.Kind -eq "Activity" } | Sort-Object Name)
$fragments = @($classRows | Where-Object { $_.Kind -eq "Fragment" } | Sort-Object Name)
$dialogs = @($classRows | Where-Object { $_.Kind -eq "Dialog" } | Sort-Object Name)

$stringFiles = @()
$resRootFull = Resolve-RepoPath $ResRoot
if (Test-Path -LiteralPath $resRootFull) {
    $stringFiles = Get-ChildItem -LiteralPath $resRootFull -Recurse -File -Filter "strings.xml" |
        Where-Object { (Split-Path -Leaf (Split-Path -Parent $_.FullName)) -match "^values" } |
        ForEach-Object { Get-RelativePath $_.FullName } |
        Sort-Object
}

$featureDirs = @()
$namespacePath = if ($namespace) { $namespace.Replace(".", "\") } else { "com\kuaigeglobal\epnova" }
$featureRoot = Resolve-RepoPath (Join-Path "app/src/main/java" (Join-Path $namespacePath "feature"))
if (Test-Path -LiteralPath $featureRoot) {
    $featureDirs = Get-ChildItem -LiteralPath $featureRoot -Directory |
        ForEach-Object {
            $count = @(Get-ChildItem -LiteralPath $_.FullName -Recurse -File -Include "*.kt", "*.java" | Where-Object { $_.Name -ne "OssManager.kt" }).Count
            [pscustomobject]@{ Name = $_.Name; Count = $count; File = Get-RelativePath $_.FullName }
        } |
        Sort-Object Name
}

$now = Get-Date -Format "yyyy-MM-dd HH:mm:ss zzz"
$markdown = New-Object System.Collections.Generic.List[string]

$markdown.Add("# Code Interface Scan Summary") | Out-Null
$markdown.Add("") | Out-Null
$markdown.Add("Generated: $now") | Out-Null
$markdown.Add("Source root: ``$SourceRoot``") | Out-Null
$markdown.Add("Excluded: ``**/OssManager.kt``") | Out-Null
$markdown.Add("") | Out-Null
$markdown.Add("This generated summary is a navigation index. Verify current files with ``rg`` before changing API, SDK, Room, Manifest, Gradle, routing, or language resources.") | Out-Null
$markdown.Add("") | Out-Null
$markdown.Add("## Project Identity") | Out-Null
$markdown.Add("") | Out-Null
$markdown.Add("| Item | Value |") | Out-Null
$markdown.Add("| --- | --- |") | Out-Null
$markdown.Add("| namespace | ``$namespace`` |") | Out-Null
$markdown.Add("| applicationId | ``$applicationId`` |") | Out-Null
$markdown.Add("| application name | ``$applicationName`` |") | Out-Null
$markdown.Add("| modules | ``$($modules -join ', ')`` |") | Out-Null
$markdown.Add("") | Out-Null
$markdown.Add("## Counts") | Out-Null
$markdown.Add("") | Out-Null
$markdown.Add("| Item | Count |") | Out-Null
$markdown.Add("| --- | ---: |") | Out-Null
$markdown.Add("| Kotlin files | $kotlinCount |") | Out-Null
$markdown.Add("| Java files | $javaCount |") | Out-Null
$markdown.Add("| Activity classes | $($activities.Count) |") | Out-Null
$markdown.Add("| Fragment classes | $($fragments.Count) |") | Out-Null
$markdown.Add("| ViewModel classes | $($viewModels.Count) |") | Out-Null
$markdown.Add("| Repository classes | $($repositories.Count) |") | Out-Null
$markdown.Add("| Dialog classes | $($dialogs.Count) |") | Out-Null
$markdown.Add("| ApiService suspend methods | $($apiMethods.Count) |") | Out-Null
$markdown.Add("") | Out-Null

$markdown.Add("## Feature Directories") | Out-Null
$markdown.Add("") | Out-Null
$markdown.Add("| Feature | Files | Path |") | Out-Null
$markdown.Add("| --- | ---: | --- |") | Out-Null
foreach ($feature in $featureDirs) {
    $markdown.Add("| $($feature.Name) | $($feature.Count) | ``$($feature.File)`` |") | Out-Null
}
$markdown.Add("") | Out-Null

$markdown.Add("## Core API Files") | Out-Null
$markdown.Add("") | Out-Null
$markdown.Add("- ``$(Get-RelativePath $apiServiceFile)``") | Out-Null
$markdown.Add("- ``$(Get-RelativePath $apiConstantsFile)``") | Out-Null
$markdown.Add("") | Out-Null

$markdown.Add("## ApiService Methods") | Out-Null
$markdown.Add("") | Out-Null
$markdown.Add("| Method | Line | Annotation |") | Out-Null
$markdown.Add("| --- | ---: | --- |") | Out-Null
foreach ($method in ($apiMethods | Select-Object -First 160)) {
    $annotation = $method.Annotation.Replace("|", "\|")
    $markdown.Add("| ``$($method.Name)`` | $($method.Line) | ``$annotation`` |") | Out-Null
}
if ($apiMethods.Count -gt 160) {
    $markdown.Add("| ... | ... | Output truncated at 160 methods. |") | Out-Null
}
$markdown.Add("") | Out-Null

$markdown.Add("## Repositories") | Out-Null
$markdown.Add("") | Out-Null
$markdown.Add("| Class | File |") | Out-Null
$markdown.Add("| --- | --- |") | Out-Null
foreach ($repo in $repositories) {
    $markdown.Add("| ``$($repo.Name)`` | ``$($repo.File)`` |") | Out-Null
}
$markdown.Add("") | Out-Null

$markdown.Add("## Language Resources") | Out-Null
$markdown.Add("") | Out-Null
foreach ($stringFile in $stringFiles) {
    $markdown.Add("- ``$stringFile``") | Out-Null
}
$markdown.Add("") | Out-Null

$markdown.Add("## Useful Verification Commands") | Out-Null
$markdown.Add("") | Out-Null
$markdown.Add("``````powershell") | Out-Null
$markdown.Add("rg -n `"class .*ViewModel|class .*Repository|class .*Adapter|class .*Dialog`" app/src/main/java -g `"!**/OssManager.kt`"") | Out-Null
$markdown.Add("rg -n `"suspend fun|@GET|@POST|@Body|ApiResponse<`" app/src/main/java -g `"!**/OssManager.kt`"") | Out-Null
$markdown.Add("rg --files app/src/main/res | rg `"values.*[/\\]strings\.xml$`"") | Out-Null
$markdown.Add("``````") | Out-Null

$output = $markdown -join "`r`n"

if ($ComparePath) {
    $resolvedComparePath = Resolve-RepoPath $ComparePath
    if (-not (Test-Path -LiteralPath $resolvedComparePath)) {
        throw "ComparePath does not exist: $ComparePath"
    }

    $oldLines = Get-NormalizedSummaryLines (Read-TextLines $resolvedComparePath)
    $newLines = Get-NormalizedSummaryLines ($output -split "`r?`n")
    $diff = @(Compare-Object -ReferenceObject $oldLines -DifferenceObject $newLines -SyncWindow 8)

    Write-Host "Code interface summary comparison"
    Write-Host "ComparePath: $(Get-RelativePath $resolvedComparePath)"
    Write-Host "Current source root: $SourceRoot"
    Write-Host "Diff entries: $($diff.Count)"

    if ($diff.Count -gt 0) {
        Write-Host ""
        Write-Host "Diff:"
        $diff |
            Select-Object -First $MaxDiffLines |
            ForEach-Object {
                $prefix = if ($_.SideIndicator -eq "=>") { "+" } else { "-" }
                Write-Host ("{0} {1}" -f $prefix, $_.InputObject)
            }

        if ($diff.Count -gt $MaxDiffLines) {
            Write-Host ""
            Write-Host "Output truncated. Increase -MaxDiffLines to see more."
        }
    }

    if ($FailOnDiff -and $diff.Count -gt 0) {
        exit 1
    }
} elseif ($WriteSummary) {
    if (-not $OutputPath) {
        $OutputPath = "design/code-interface-scan-summary.generated.md"
    }
    $resolvedOutput = Resolve-RepoPath $OutputPath
    $outputDir = Split-Path -Parent $resolvedOutput
    if (-not (Test-Path -LiteralPath $outputDir)) {
        New-Item -ItemType Directory -Path $outputDir | Out-Null
    }
    Set-Content -LiteralPath $resolvedOutput -Value $output -Encoding UTF8
    Write-Host "Wrote summary: $(Get-RelativePath $resolvedOutput)"
} else {
    Write-Output $output
}
