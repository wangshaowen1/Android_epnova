[CmdletBinding()]
param(
    [string]$SourceDesign,
    [string]$TargetDesign = "D:\workspace\FastFilm\dev\design",
    [switch]$Apply,
    [switch]$All,
    [switch]$ChangedOnly,
    [switch]$IncludeProjectSnapshot,
    [switch]$IncludeProjectDocs,
    [string[]]$Include,
    [string[]]$Exclude = @()
)

if (-not $SourceDesign) {
    $SourceDesign = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
}

$script = Join-Path $PSScriptRoot "sync_design_docs.ps1"
$params = @{
    SourceDesign = $SourceDesign
    TargetDesign = $TargetDesign
}

if ($Apply) { $params.Apply = $true }
if ($All) { $params.All = $true }
if ($ChangedOnly) { $params.ChangedOnly = $true }
if ($IncludeProjectSnapshot) { $params.IncludeProjectSnapshot = $true }
if ($IncludeProjectDocs) { $params.IncludeProjectDocs = $true }
if ($Include -and $Include.Count -gt 0) { $params.Include = $Include }
if ($Exclude -and $Exclude.Count -gt 0) { $params.Exclude = $Exclude }

& $script @params
