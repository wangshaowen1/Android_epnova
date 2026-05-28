[CmdletBinding()]
param(
    [string]$RootPath = "app/src/main/res",
    [string[]]$Path,
    [switch]$ChangedOnly,
    [switch]$ChangedLinesOnly,
    [switch]$IncludeTextSize,
    [int]$MaxIssues = 200,
    [switch]$FailOnIssue
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

function Add-Issue {
    param(
        [System.Collections.Generic.List[object]]$Issues,
        [string]$File,
        [int]$Line,
        [string]$Severity,
        [string]$Rule,
        [string]$Message,
        [string]$Suggestion
    )

    $Issues.Add([pscustomobject]@{
        Severity = $Severity
        Rule = $Rule
        File = $File
        Line = $Line
        Message = $Message
        Suggestion = $Suggestion
    }) | Out-Null
}

function Invoke-GitOutput {
    param([string[]]$Arguments)

    $oldErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        return (& git @Arguments 2>&1) |
            Where-Object { $_ -is [string] -and $_ -notmatch "^warning:" }
    } finally {
        $ErrorActionPreference = $oldErrorActionPreference
    }
}

function Get-XmlFilesFromPath {
    param([string[]]$InputPaths)

    $result = New-Object System.Collections.Generic.List[string]
    foreach ($item in $InputPaths) {
        $resolved = Resolve-RepoPath $item
        if (-not (Test-Path -LiteralPath $resolved)) {
            Write-Warning "Path does not exist: $item"
            continue
        }

        $target = Get-Item -LiteralPath $resolved
        if ($target.PSIsContainer) {
            Get-ChildItem -LiteralPath $target.FullName -Recurse -File -Filter "*.xml" |
                ForEach-Object { $result.Add($_.FullName) | Out-Null }
        } elseif ($target.Extension -ieq ".xml") {
            $result.Add($target.FullName) | Out-Null
        }
    }
    return $result
}

function Get-ChangedXmlFiles {
    Push-Location $RepoRoot
    try {
        $files = @()
        $files += Invoke-GitOutput @("diff", "--name-only", "--diff-filter=ACMR", "--", "app/src/main/res")
        $files += Invoke-GitOutput @("diff", "--cached", "--name-only", "--diff-filter=ACMR", "--", "app/src/main/res")
        $files += Invoke-GitOutput @("ls-files", "--others", "--exclude-standard", "--", "app/src/main/res")
        return $files |
            Where-Object { $_ -match "\.xml$" } |
            Sort-Object -Unique |
            ForEach-Object { Resolve-RepoPath $_ } |
            Where-Object { Test-Path -LiteralPath $_ }
    } finally {
        Pop-Location
    }
}

function Add-LineRange {
    param(
        [hashtable]$LineMap,
        [string]$RelativePath,
        [int]$Start,
        [int]$Count
    )

    if ($Count -le 0) {
        return
    }
    if (-not $LineMap.ContainsKey($RelativePath)) {
        $LineMap[$RelativePath] = [System.Collections.Generic.HashSet[int]]::new()
    }

    for ($lineNumber = $Start; $lineNumber -lt ($Start + $Count); $lineNumber++) {
        [void]$LineMap[$RelativePath].Add($lineNumber)
    }
}

function Get-ChangedXmlLineMap {
    $lineMap = @{}

    Push-Location $RepoRoot
    try {
        $diffLines = @()
        $diffLines += Invoke-GitOutput @("diff", "--unified=0", "--diff-filter=ACMR", "--", "app/src/main/res")
        $diffLines += Invoke-GitOutput @("diff", "--cached", "--unified=0", "--diff-filter=ACMR", "--", "app/src/main/res")

        $currentRelativePath = $null
        foreach ($diffLine in $diffLines) {
            if ($diffLine -match "^\+\+\+ b/(.+\.xml)$") {
                $candidate = Resolve-RepoPath $Matches[1]
                $currentRelativePath = if (Test-Path -LiteralPath $candidate) { Get-RelativePath $candidate } else { $null }
                continue
            }

            if ($currentRelativePath -and $diffLine -match "^@@ -\d+(?:,\d+)? \+(?<start>\d+)(?:,(?<count>\d+))? @@") {
                $start = [int]$Matches["start"]
                $count = if ($Matches["count"]) { [int]$Matches["count"] } else { 1 }
                Add-LineRange $lineMap $currentRelativePath $start $count
            }
        }

        $untrackedFiles = Invoke-GitOutput @("ls-files", "--others", "--exclude-standard", "--", "app/src/main/res") |
            Where-Object { $_ -match "\.xml$" } |
            Sort-Object -Unique

        foreach ($untrackedFile in $untrackedFiles) {
            $fullPath = Resolve-RepoPath $untrackedFile
            if (-not (Test-Path -LiteralPath $fullPath)) {
                continue
            }
            $relative = Get-RelativePath $fullPath
            $lineCount = @(Get-Content -LiteralPath $fullPath -Encoding UTF8).Count
            Add-LineRange $lineMap $relative 1 $lineCount
        }
    } finally {
        Pop-Location
    }

    return $lineMap
}

function Get-DefaultXmlFiles {
    $resolvedRoot = Resolve-RepoPath $RootPath
    if (-not (Test-Path -LiteralPath $resolvedRoot)) {
        throw "RootPath does not exist: $RootPath"
    }

    Get-ChildItem -LiteralPath $resolvedRoot -Recurse -File -Filter "*.xml" |
        Where-Object {
            $parent = Split-Path -Leaf (Split-Path -Parent $_.FullName)
            $parent -match "^(layout|drawable)(-|$)"
        } |
        ForEach-Object { $_.FullName }
}

function Test-ShouldReportIssue {
    param(
        [string]$RelativePath,
        [int]$Line,
        [switch]$FileLevel
    )

    if (-not $ChangedLinesOnly) {
        return $true
    }
    if (-not $changedLineMap.ContainsKey($RelativePath)) {
        return $false
    }

    $changedLines = $changedLineMap[$RelativePath]
    if ($FileLevel) {
        return $changedLines.Count -gt 0
    }
    return $changedLines.Contains($Line)
}

if ($ChangedLinesOnly) {
    $ChangedOnly = $true
}

$changedLineMap = if ($ChangedLinesOnly) { Get-ChangedXmlLineMap } else { @{} }

$targetFiles = if ($Path -and $Path.Count -gt 0) {
    Get-XmlFilesFromPath $Path
} elseif ($ChangedOnly) {
    Get-ChangedXmlFiles
} else {
    Get-DefaultXmlFiles
}

$targetFiles = @($targetFiles | Sort-Object -Unique)
$issues = New-Object System.Collections.Generic.List[object]

$spacingTokens = @{
    "4" = "@dimen/spacing_xs"
    "8" = "@dimen/spacing_s"
    "12" = "@dimen/spacing_m"
    "16" = "@dimen/spacing_l"
    "24" = "@dimen/spacing_xl"
}
$radiusTokens = @{
    "4" = "@dimen/radius_s"
    "8" = "@dimen/radius_m"
    "12" = "@dimen/radius_l"
    "16" = "@dimen/radius_xl"
}

foreach ($file in $targetFiles) {
    $relative = Get-RelativePath $file
    $parent = Split-Path -Leaf (Split-Path -Parent $file)
    $isDrawable = $parent -match "^drawable(-|$)"

    try {
        $lines = Get-Content -LiteralPath $file -Encoding UTF8
    } catch {
        $lines = Get-Content -LiteralPath $file
    }

    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        $lineNumber = $i + 1
        $trimmed = $line.Trim()
        if ($trimmed.StartsWith("<!--")) {
            continue
        }

        if ($line -match "#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?") {
            if (Test-ShouldReportIssue $relative $lineNumber) {
                Add-Issue $issues $relative $lineNumber "WARN" "bare-color" `
                    "Bare hex color found in XML." `
                    "Use ?attr, @color, or an existing drawable when the value maps to project tokens."
            }
        }

        if ($line -match "(?<attr>(android:|app:)?[\w_]*(layout_margin\w*|padding\w*|itemSpacing|dividerHeight|layout_goneMargin\w*))\s*=\s*`"(?<value>4|8|12|16|24)dp`"") {
            $value = $Matches["value"]
            if (Test-ShouldReportIssue $relative $lineNumber) {
                Add-Issue $issues $relative $lineNumber "WARN" "spacing-token" `
                    "Common spacing value ${value}dp should use a project spacing token when possible." `
                    "Prefer $($spacingTokens[$value])."
            }
        }

        if ($line -match "(?<attr>(android:|app:)?[\w_]*(radius|cornerRadius|cardCornerRadius))\s*=\s*`"(?<value>4|8|12|16)dp`"") {
            $value = $Matches["value"]
            if (Test-ShouldReportIssue $relative $lineNumber) {
                Add-Issue $issues $relative $lineNumber "WARN" "radius-token" `
                    "Common radius value ${value}dp should use a project radius token when possible." `
                    "Prefer $($radiusTokens[$value])."
            }
        }

        if ($line -match "android:fontFamily\s*=\s*`"([^`"]*(sans-serif|PingFang SC)[^`"]*)`"") {
            if (Test-ShouldReportIssue $relative $lineNumber) {
                Add-Issue $issues $relative $lineNumber "WARN" "font-fallback" `
                    "System or design font fallback found." `
                    "Use project Text styles instead of sans-serif, sans-serif-medium, or PingFang SC."
            }
        }

        if ($line -match "android:(letterSpacing|lineHeight)\s*=") {
            if (Test-ShouldReportIssue $relative $lineNumber) {
                Add-Issue $issues $relative $lineNumber "INFO" "text-metric" `
                    "letterSpacing or lineHeight is set locally." `
                    "Keep it only when the design needs a special text metric and no project Text style matches."
            }
        }

        if ($IncludeTextSize -and $line -match "android:textSize\s*=\s*`"\d+sp`"") {
            if (Test-ShouldReportIssue $relative $lineNumber) {
                Add-Issue $issues $relative $lineNumber "INFO" "text-size" `
                    "Local textSize found." `
                    "Prefer project Text styles when an exact style exists."
            }
        }
    }

    if ($isDrawable) {
        $fileName = [System.IO.Path]::GetFileName($file).ToLowerInvariant()
        if ($fileName -match "^(bg_(gray|white|black|card|item|normal|selected|default)|shape_\d+|gradient_button)\.xml$") {
            if (Test-ShouldReportIssue $relative 1 -FileLevel) {
                Add-Issue $issues $relative 1 "WARN" "drawable-generic-name" `
                    "Generic drawable name is hard to search and reuse." `
                    "Prefer value-based names such as bg_242435_r16.xml or a clear business asset name."
            }
        }

        $content = ($lines -join "`n")
        $isSimpleShape =
            $content -match "<shape\b" -and
            $content -notmatch "<(selector|vector|layer-list|animated-vector|ripple)\b" -and
            $content -match "<solid\b" -and
            $content -match "<corners\b"
        $hasLiteralColor = $content -match "android:(color|startColor|endColor|centerColor)\s*=\s*`"#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?`""
        $allowedValueName =
            $fileName -match "^bg_[0-9a-f]{6}_(r|tr|br)\d+\.xml$" -or
            $fileName -match "^bg_\d{2}_[0-9a-f]{6}_(r|tr|br)\d+\.xml$" -or
            $fileName -match "^bg_[0-9a-f]{6}_stroke_.*_(r|tr|br)\d+\.xml$" -or
            $fileName -match "^bg_\d{2}_[0-9a-f]{6}_stroke_.*_(r|tr|br)\d+\.xml$" -or
            $fileName -match "^fill_color\d+_(r|tr|br)\d+\.xml$" -or
            $fileName -match "^shape_bg_[a-z0-9_]+\.xml$"

        if ($isSimpleShape -and $hasLiteralColor -and -not $allowedValueName) {
            if (Test-ShouldReportIssue $relative 1 -FileLevel) {
                Add-Issue $issues $relative 1 "WARN" "drawable-value-name" `
                    "Simple solid/corner drawable does not look value-based." `
                    "Use bg_<color>_<corner><radius>.xml or bg_<percent>_<color>_<corner><radius>.xml unless this is a special semantic asset."
            }
        }
    }
}

$grouped = $issues | Group-Object Rule | Sort-Object Name

Write-Host "Android XML token check"
Write-Host "Repo: $RepoRoot"
if ($ChangedLinesOnly) {
    Write-Host "Mode: changed lines only"
} elseif ($ChangedOnly) {
    Write-Host "Mode: changed files"
}
Write-Host "Files scanned: $($targetFiles.Count)"
Write-Host "Issues found: $($issues.Count)"

if ($grouped.Count -gt 0) {
    Write-Host ""
    Write-Host "Issue counts:"
    foreach ($group in $grouped) {
        Write-Host ("- {0}: {1}" -f $group.Name, $group.Count)
    }
}

if ($issues.Count -gt 0) {
    Write-Host ""
    Write-Host "Issues:"
    $issues |
        Select-Object -First $MaxIssues |
        ForEach-Object {
            Write-Host ("{0} {1}:{2} [{3}] {4}" -f $_.Severity, $_.File, $_.Line, $_.Rule, $_.Message)
            Write-Host ("  Suggestion: {0}" -f $_.Suggestion)
        }

    if ($issues.Count -gt $MaxIssues) {
        Write-Host ""
        Write-Host "Output truncated. Increase -MaxIssues to see more."
    }
}

if ($FailOnIssue -and $issues.Count -gt 0) {
    exit 1
}
