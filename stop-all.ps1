param(
    [switch]$WithDocker
)

$ErrorActionPreference = "Continue"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Ports = @(9000, 9101, 9102, 9103, 9104, 9105, 5173)

Set-Location -LiteralPath $Root

foreach ($port in $Ports) {
    $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($connection in $connections) {
        $pidValue = $connection.OwningProcess
        if ($pidValue -and $pidValue -ne 0) {
            $process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
            if ($process) {
                Write-Host "[stop] port $port pid=$pidValue"
                Stop-Process -Id $pidValue -Force -ErrorAction SilentlyContinue
            }
        }
    }
}

if ($WithDocker) {
    Write-Host "[stop] Docker middleware"
    docker compose down
}

Write-Host "OfficeFlow services stopped."
