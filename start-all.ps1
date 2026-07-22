param(
    [switch]$SkipBuild,
    [switch]$NoDocker,
    [switch]$NoFrontend,
    [switch]$BackendOnly
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$BackendDir = Join-Path $Root "backend"
$FrontendDir = Join-Path $Root "frontend"

function Require-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command not found: $Name"
    }
}

function Quote-PowerShellString {
    param([string]$Value)
    return "'" + $Value.Replace("'", "''") + "'"
}

function Test-PortListening {
    param([int]$Port)
    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    return $null -ne $connection
}

function Start-ServiceWindow {
    param(
        [string]$Name,
        [string]$WorkingDirectory,
        [string]$Command
    )

    $title = "OfficeFlow - $Name"
    $quotedTitle = Quote-PowerShellString $title
    $quotedWorkingDirectory = Quote-PowerShellString $WorkingDirectory
    $windowCommand = "`$Host.UI.RawUI.WindowTitle = $quotedTitle; Set-Location -LiteralPath $quotedWorkingDirectory; $Command"

    Start-Process `
        -FilePath "powershell.exe" `
        -ArgumentList @("-NoExit", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $windowCommand) `
        -PassThru | Out-Null
}

Set-Location -LiteralPath $Root

Require-Command "mvn"
if (-not $NoDocker) {
    Require-Command "docker"
}
if (-not $NoFrontend -and -not $BackendOnly) {
    Require-Command "pnpm"
}

if (-not $NoDocker) {
    Write-Host "[1/4] Starting Docker middleware..."
    docker compose up -d
} else {
    Write-Host "[1/4] Skip Docker middleware."
}

if (-not $SkipBuild) {
    Write-Host "[2/4] Installing backend modules..."
    Push-Location $BackendDir
    try {
        mvn clean install -DskipTests
    } finally {
        Pop-Location
    }
} else {
    Write-Host "[2/4] Skip backend build."
}

if (-not $NoFrontend -and -not $BackendOnly) {
    if (-not (Test-Path (Join-Path $FrontendDir "node_modules"))) {
        Write-Host "[3/4] Installing frontend dependencies..."
        Push-Location $FrontendDir
        try {
            pnpm install
        } finally {
            Pop-Location
        }
    } else {
        Write-Host "[3/4] Frontend dependencies already installed."
    }
} else {
    Write-Host "[3/4] Skip frontend dependencies."
}

$services = @(
    @{ name = "oa-gateway"; port = 9000; workdir = $BackendDir; command = "mvn -pl oa-gateway spring-boot:run" },
    @{ name = "user-service"; port = 9101; workdir = $BackendDir; command = "mvn -pl user-service spring-boot:run" },
    @{ name = "attendance-service"; port = 9102; workdir = $BackendDir; command = "mvn -pl attendance-service spring-boot:run" },
    @{ name = "flow-service"; port = 9103; workdir = $BackendDir; command = "mvn -pl flow-service spring-boot:run" },
    @{ name = "notice-service"; port = 9104; workdir = $BackendDir; command = "mvn -pl notice-service spring-boot:run" },
    @{ name = "report-service"; port = 9105; workdir = $BackendDir; command = "mvn -pl report-service spring-boot:run" }
)

if (-not $NoFrontend -and -not $BackendOnly) {
    $services += @{ name = "frontend"; port = 5173; workdir = $FrontendDir; command = "pnpm dev" }
}

Write-Host "[4/4] Starting application windows..."
foreach ($service in $services) {
    if (Test-PortListening $service.port) {
        Write-Host "[skip] $($service.name): port $($service.port) is already listening."
        continue
    }
    Write-Host "[start] $($service.name) on port $($service.port)"
    Start-ServiceWindow -Name $service.name -WorkingDirectory $service.workdir -Command $service.command
    Start-Sleep -Milliseconds 500
}

Write-Host ""
Write-Host "OfficeFlow startup commands have been opened."
Write-Host "Frontend: http://127.0.0.1:5173/"
Write-Host "Gateway:  http://127.0.0.1:9000"
Write-Host "Nacos:    http://127.0.0.1:8848/nacos"
Write-Host ""
Write-Host "Stop app services: .\stop-all.cmd"
Write-Host "Stop app services and Docker middleware: .\stop-all.cmd -WithDocker"
