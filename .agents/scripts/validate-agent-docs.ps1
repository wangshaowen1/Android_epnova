[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$script:Errors = [System.Collections.Generic.List[string]]::new()
$script:Warnings = [System.Collections.Generic.List[string]]::new()

$agentsRoot = Split-Path -Parent $PSScriptRoot
$repoRoot = Split-Path -Parent $agentsRoot
$skillsRoot = Join-Path $agentsRoot "skills"
$readmePath = Join-Path $agentsRoot "README.md"
$repoPrefix = $repoRoot.TrimEnd("\", "/") + [IO.Path]::DirectorySeparatorChar

function Get-RepoRelativePath([string]$Path) {
    $fullPath = [IO.Path]::GetFullPath($Path)
    if ($fullPath.StartsWith($repoPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        return $fullPath.Substring($repoPrefix.Length).Replace("\", "/")
    }
    return $fullPath
}

function Add-ValidationError([string]$Path, [string]$Message) {
    $script:Errors.Add("$(Get-RepoRelativePath $Path): $Message")
}

function Add-ValidationWarning([string]$Path, [string]$Message) {
    $script:Warnings.Add("$(Get-RepoRelativePath $Path): $Message")
}

function Read-Utf8Strict([string]$Path, [switch]$RejectBom) {
    $bytes = [IO.File]::ReadAllBytes($Path)
    if ($RejectBom -and $bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        Add-ValidationError $Path "UTF-8 BOM found; frontmatter must start at the first byte"
    }

    try {
        return [Text.UTF8Encoding]::new($false, $true).GetString($bytes)
    }
    catch {
        Add-ValidationError $Path "invalid UTF-8 text"
        return [Text.Encoding]::UTF8.GetString($bytes)
    }
}

function Get-FrontmatterValue([string[]]$Lines, [string]$Key) {
    $line = $Lines | Where-Object { $_ -match "^$([regex]::Escape($Key)):\s*(.+)$" } | Select-Object -First 1
    if ($null -eq $line) { return $null }
    $value = ($line -replace "^$([regex]::Escape($Key)):\s*", "").Trim()
    if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
        return $value.Substring(1, $value.Length - 2)
    }
    return $value
}

if (-not (Test-Path -LiteralPath $skillsRoot -PathType Container)) {
    throw "Skills root not found: $skillsRoot"
}

$skillDirs = @(Get-ChildItem -LiteralPath $skillsRoot -Directory | Sort-Object Name)
foreach ($skillDir in $skillDirs) {
    $skillPath = Join-Path $skillDir.FullName "SKILL.md"
    if (-not (Test-Path -LiteralPath $skillPath -PathType Leaf)) {
        Add-ValidationError $skillDir.FullName "missing SKILL.md"
        continue
    }

    $text = Read-Utf8Strict $skillPath -RejectBom
    $lines = @($text -split "`r?`n")
    if ($lines.Count -lt 4 -or $lines[0] -ne "---") {
        Add-ValidationError $skillPath "frontmatter must start on the first line"
        continue
    }

    $frontmatterEnd = -1
    for ($index = 1; $index -lt $lines.Count; $index++) {
        if ($lines[$index] -eq "---") {
            $frontmatterEnd = $index
            break
        }
    }
    if ($frontmatterEnd -lt 0) {
        Add-ValidationError $skillPath "frontmatter closing delimiter is missing"
        continue
    }

    $frontmatterLines = @($lines[1..($frontmatterEnd - 1)])
    $frontmatterKeys = @($frontmatterLines | Where-Object { $_ -match "^([a-zA-Z0-9_-]+):" } | ForEach-Object { $Matches[1] })
    $unknownKeys = @($frontmatterKeys | Where-Object { $_ -notin @("name", "description") })
    if ($unknownKeys.Count -gt 0) {
        Add-ValidationError $skillPath "frontmatter allows only name/description; found: $($unknownKeys -join ', ')"
    }

    $name = Get-FrontmatterValue $frontmatterLines "name"
    $description = Get-FrontmatterValue $frontmatterLines "description"
    if ([string]::IsNullOrWhiteSpace($name)) {
        Add-ValidationError $skillPath "name is missing"
    }
    elseif ($name -notmatch "^[a-z0-9-]+$" -or $name.Length -gt 64) {
        Add-ValidationError $skillPath "name must use lowercase letters, digits, or hyphens and be at most 64 characters"
    }
    elseif ($name -ne $skillDir.Name) {
        Add-ValidationError $skillPath "name '$name' does not match directory '$($skillDir.Name)'"
    }

    if ([string]::IsNullOrWhiteSpace($description)) {
        Add-ValidationError $skillPath "description is missing"
    }
    elseif ($description.Contains("<") -or $description.Contains(">")) {
        Add-ValidationError $skillPath "description must not contain angle brackets"
    }

    $body = ($lines[($frontmatterEnd + 1)..($lines.Count - 1)] -join "`n")
    $triggerHeadings = @(
        -join @([char]0x89E6, [char]0x53D1, [char]0x6761, [char]0x4EF6),
        -join @([char]0x4F55, [char]0x65F6, [char]0x4F7F, [char]0x7528),
        -join @([char]0x4F7F, [char]0x7528, [char]0x65F6, [char]0x673A)
    )
    $triggerHeadingPattern = ($triggerHeadings | ForEach-Object { [regex]::Escape($_) }) -join "|"
    if ($body -match "(?m)^#{1,6}\s*($triggerHeadingPattern)\s*$") {
        Add-ValidationError $skillPath "trigger conditions belong in description, not a body heading"
    }
    if ($lines.Count -gt 500) {
        Add-ValidationWarning $skillPath "$($lines.Count) lines; consider reducing or moving details to references"
    }

    $referencesRoot = Join-Path $skillDir.FullName "references"
    if (Test-Path -LiteralPath $referencesRoot -PathType Container) {
        foreach ($reference in Get-ChildItem -LiteralPath $referencesRoot -File -Recurse) {
            if ($reference.DirectoryName -ne $referencesRoot) {
                Add-ValidationError $reference.FullName "references supports one directory level only"
            }
            $referenceLink = "references/$($reference.Name)"
            if (-not $text.Contains($referenceLink)) {
                Add-ValidationError $reference.FullName "not linked directly from the sibling SKILL.md"
            }
        }
    }

    $metadataPath = Join-Path $skillDir.FullName "agents/openai.yaml"
    if (-not (Test-Path -LiteralPath $metadataPath -PathType Leaf)) {
        Add-ValidationError $skillDir.FullName "missing agents/openai.yaml"
    }
    else {
        $metadata = Read-Utf8Strict $metadataPath
        foreach ($field in @("display_name", "short_description", "default_prompt")) {
            $metadataPattern = '(?m)^\s*' + [regex]::Escape($field) + '\s*:\s*"[^"]+"\s*$'
            if ($metadata -notmatch $metadataPattern) {
                Add-ValidationError $metadataPath "$field must exist and use double quotes"
            }
        }
        if (-not [string]::IsNullOrWhiteSpace($name) -and -not $metadata.Contains('$' + $name)) {
            Add-ValidationError $metadataPath ("default_prompt must reference '$" + $name + "'")
        }
    }
}

if (-not (Test-Path -LiteralPath $readmePath -PathType Leaf)) {
    Add-ValidationError $agentsRoot "missing .agents/README.md"
}
else {
    $readme = Read-Utf8Strict $readmePath
    $registeredSkills = @([regex]::Matches($readme, '(?m)^\|\s*`(?<name>[a-z0-9-]+)`\s*\|') | ForEach-Object { $_.Groups["name"].Value } | Sort-Object -Unique)
    $directorySkills = @($skillDirs.Name | Sort-Object -Unique)
    foreach ($missing in @($directorySkills | Where-Object { $_ -notin $registeredSkills })) {
        Add-ValidationError $readmePath "skill registry is missing '$missing'"
    }
    foreach ($stale in @($registeredSkills | Where-Object { $_ -notin $directorySkills })) {
        Add-ValidationError $readmePath "skill registry contains missing directory '$stale'"
    }
}

$markdownFiles = [System.Collections.Generic.List[IO.FileInfo]]::new()
foreach ($rootFile in @("AGENTS.md", "CLAUDE.md")) {
    $candidate = Join-Path $repoRoot $rootFile
    if (Test-Path -LiteralPath $candidate -PathType Leaf) {
        $markdownFiles.Add((Get-Item -LiteralPath $candidate))
    }
}
foreach ($directory in @($agentsRoot, (Join-Path $repoRoot "docs"))) {
    if (Test-Path -LiteralPath $directory -PathType Container) {
        foreach ($file in Get-ChildItem -LiteralPath $directory -Filter "*.md" -File -Recurse) {
            $markdownFiles.Add($file)
        }
    }
}

foreach ($markdownFile in $markdownFiles) {
    $markdown = Read-Utf8Strict $markdownFile.FullName
    foreach ($match in [regex]::Matches($markdown, "\[[^\]]*\]\((?<target>[^)]+)\)")) {
        $target = $match.Groups["target"].Value.Trim().Trim("<", ">")
        if ($target -match "^(https?://|mailto:|#)" -or [string]::IsNullOrWhiteSpace($target)) { continue }
        $target = ($target -split "#", 2)[0]
        if ([IO.Path]::IsPathRooted($target) -or $target -match "^[A-Za-z]:[\\/]") {
            Add-ValidationError $markdownFile.FullName "Markdown link must not use a machine-absolute path: $target"
            continue
        }
        $resolvedTarget = [IO.Path]::GetFullPath((Join-Path $markdownFile.DirectoryName ([Uri]::UnescapeDataString($target))))
        if (-not (Test-Path -LiteralPath $resolvedTarget)) {
            Add-ValidationError $markdownFile.FullName "local Markdown link does not exist: $target"
        }
    }
}

Write-Host "Project skills: $($skillDirs.Count)"
foreach ($warning in $script:Warnings) { Write-Warning $warning }
foreach ($validationError in $script:Errors) { Write-Error $validationError -ErrorAction Continue }

if ($script:Errors.Count -gt 0) {
    Write-Host "Validation failed: $($script:Errors.Count) error(s), $($script:Warnings.Count) warning(s)."
    exit 1
}

Write-Host "Validation passed: 0 errors, $($script:Warnings.Count) warning(s)."
