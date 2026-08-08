$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

Set-Location $PSScriptRoot

function Invoke-Step {
    param(
        [Parameter(Mandatory)]
        [string]$Name,
        [Parameter(Mandatory)]
        [scriptblock]$Action
    )

    Write-Host "==> $Name"
    & $Action

    if ($LASTEXITCODE -ne 0) {
        throw "$Name failed with exit code $LASTEXITCODE"
    }
}

Invoke-Step 'Verify Docker Desktop is running' {
    $timeoutSeconds = 60
    $deadline = (Get-Date).AddSeconds($timeoutSeconds)
    $dockerDesktop = Get-Process | Where-Object {
        $_.ProcessName -like 'Docker Desktop*' -or $_.ProcessName -like 'com.docker.backend*'
    }

    if (-not $dockerDesktop) {
        throw 'Docker Desktop is not running. Start Docker Desktop and try again.'
    }

    while ((Get-Date) -lt $deadline) {
        docker info | Out-Null

        if ($LASTEXITCODE -eq 0) {
            return
        }

        Start-Sleep -Seconds 2
    }

    throw "Docker Desktop didn't become ready within $timeoutSeconds seconds - please start it manually and re-run this script."
}

Invoke-Step 'Bring up full compose stack' {
    docker compose up -d --build
}

Invoke-Step 'Confirm application started' {
    $deadline = (Get-Date).AddMinutes(3)
    $successPatterns = @(
        'Started .* in .* seconds',
        'Tomcat started on port',
        'Started .*Application'
    )
    $failurePatterns = @(
        'APPLICATION FAILED TO START',
        'FlywayException',
        'Migration .* failed',
        'Validate failed',
        'failed to execute',
        'Error',
        'Exception',
        'Caused by'
    )

    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 3

        $logs = docker compose logs --no-color --tail 200 events-manager 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw 'docker compose logs failed'
        }

        if ($logs -match ($failurePatterns -join '|')) {
            throw "Full-stack startup failure found in container logs:`n$logs"
        }

        if ($logs -match ($successPatterns -join '|')) {
            Write-Host 'Application started successfully.'
            return
        }
    }

    throw 'Timed out waiting for the application container to report a successful startup message'
}

Write-Host 'Full-stack validation completed successfully.'

<#
FULL-STACK VALIDATION SCRIPT - WHAT THIS DOES

This script is for an occasional end-to-end validation run, not the daily
startup routine. It skips the git sync and local Maven build steps, starts
the entire compose stack, and waits for the app container to report a normal
startup message.

1. Verify Docker Desktop is running
   Checks that Docker Desktop is already available and waits briefly for it
to become responsive.

2. Bring up full compose stack
   Runs `docker compose up -d --build`, starting the entire stack including
   the app container.

3. Confirm application started
   Polls the app container logs for up to 3 minutes, looking for either a
   successful Spring Boot startup message or a failure pattern. Fails fast if
   the logs show startup errors instead of waiting out the full timeout.

If every step passes, prints "Full-stack validation completed successfully." -
at that point the compose stack is up and the application has started cleanly.
#>