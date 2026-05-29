# Corre los tests de integración del backend contra un Postgres EFÍMERO (Docker CLI).
# En Windows, Testcontainers no autodetecta Docker Desktop, así que levantamos el
# contenedor con el CLI, apuntamos los tests (TEST_DB_*) y lo borramos al final.
# Uso:  powershell -File scripts\run-backend-tests.ps1
$ErrorActionPreference = 'Stop'
$container = 'frutapp-pg-test'
$port = 5434

docker rm -f $container 2>$null | Out-Null
docker run -d --name $container -e POSTGRES_USER=frutapp -e POSTGRES_PASSWORD=frutapp -e POSTGRES_DB=frutapp -p "${port}:5432" postgres:16 | Out-Null
Write-Host "Esperando Postgres efímero..."
for ($i = 0; $i -lt 30; $i++) {
    docker exec $container pg_isready -U frutapp -d frutapp 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) { break }
    Start-Sleep -Seconds 1
}

$env:TEST_DB_HOST = 'localhost'
$env:TEST_DB_PORT = "$port"
$env:TEST_DB_NAME = 'frutapp'
$env:TEST_DB_USER = 'frutapp'
$env:TEST_DB_PASSWORD = 'frutapp'
if (-not $env:JAVA_HOME) { $env:JAVA_HOME = 'C:\Users\shuai\Java\jdk-17.0.19+10' }

try {
    & "$PSScriptRoot\..\gradlew.bat" -p "$PSScriptRoot\.." :backend:test --no-daemon --console=plain --rerun-tasks
} finally {
    docker rm -f $container 2>$null | Out-Null
    Write-Host "Contenedor de test eliminado (cero data basura)."
}
