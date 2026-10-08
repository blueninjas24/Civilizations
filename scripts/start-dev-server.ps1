param(
    [Parameter(Mandatory = $true)][string]$Directory,
    [string]$Java = 'java'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$serverDirectory = (Resolve-Path -LiteralPath $Directory).Path
$markerPath = Join-Path $serverDirectory 'civilizations-dev-marker.txt'
if (!(Test-Path -LiteralPath $markerPath) -or
    (Get-Content -LiteralPath $markerPath -Raw).Trim() -ne 'Disposable Civilizations Phase 1 test environment') {
    throw 'This launcher requires an explicitly marked disposable Civilizations test directory.'
}
$paperJar = Join-Path $serverDirectory 'paper-26.2-129.jar'
$expectedHash = 'b1d8f6bfa1b6101fa8e947b53041cb3bdf5540e7b83b6547ca19ba7edefeb083'
if ((Get-FileHash -LiteralPath $paperJar -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expectedHash) {
    throw 'The pinned Paper server checksum does not match.'
}
$eula = Get-Content -LiteralPath (Join-Path $serverDirectory 'eula.txt')
if (!($eula -match '^\s*eula\s*=\s*true\s*$')) {
    throw 'Review and accept https://www.minecraft.net/en-us/eula yourself before starting the server.'
}
$properties = Get-Content -LiteralPath (Join-Path $serverDirectory 'server.properties')
foreach ($requiredProperty in @('server-ip=127.0.0.1', 'server-port=25577',
    'online-mode=true', 'level-name=civilizations-phase1-test', 'enable-rcon=false', 'enable-query=false')) {
    if ($properties -notcontains $requiredProperty) {
        throw "Disposable server configuration must contain: $requiredProperty"
    }
    $propertyName = ($requiredProperty -split '=', 2)[0]
    if (@($properties -match ('^\s*' + [regex]::Escape($propertyName) + '\s*=')).Count -ne 1) {
        throw "Duplicate configuration entries for: $propertyName"
    }
}
$javaVersion = (& $Java -version 2>&1 | Out-String)
if ($LASTEXITCODE -ne 0 -or $javaVersion -notmatch '(version\s+"25[.\"]|openjdk\s+25[.\s])') {
    throw "This test environment requires Java 25. Found: $javaVersion"
}
Push-Location -LiteralPath $serverDirectory
try {
    & $Java '-Xms512M' '-Xmx1536M' '-jar' $paperJar '--nogui'
    if ($LASTEXITCODE -ne 0) { throw "Paper exited with code $LASTEXITCODE" }
} finally {
    Pop-Location
}
