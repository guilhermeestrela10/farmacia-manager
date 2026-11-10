$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath "$env:JAVA_HOME\bin\javac.exe")) {
    $jdkTeste = Get-ChildItem 'C:\Program Files\Java' -Directory |
        Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\javac.exe') } |
        Sort-Object Name -Descending | Select-Object -First 1
    if (-not $jdkTeste) { throw 'JDK não encontrado. Configure JAVA_HOME com um JDK 21 ou superior.' }
    $env:JAVA_HOME = $jdkTeste.FullName
}

$nodeTeste = (Get-Command node.exe -CommandType Application -ErrorAction Stop).Source
$arquivosJsTeste = @(Get-ChildItem -LiteralPath 'src/test/js' -Filter '*.test.cjs' | ForEach-Object { $_.FullName })
& $nodeTeste --test @arquivosJsTeste
if ($LASTEXITCODE -ne 0) { throw 'Os testes JavaScript falharam.' }

$reservaPorta = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 55433)
try { $reservaPorta.Start() }
catch { throw 'A porta 55433 está ocupada. Nenhum banco existente será reutilizado.' }
finally { $reservaPorta.Stop() }

# ZIP oficial, sem serviço Windows e sem alteração do PATH global.
$cacheMongo = Join-Path $env:LOCALAPPDATA 'farmacia-manager-test-tools'
$mongoTeste = Join-Path $cacheMongo 'mongodb-win32-x86_64-windows-8.0.32\bin\mongod.exe'
if (-not (Test-Path -LiteralPath $mongoTeste)) {
    New-Item -ItemType Directory -Path $cacheMongo -Force | Out-Null
    $zipMongo = Join-Path $cacheMongo 'mongodb-windows-x86_64-8.0.32.zip'
    if (-not (Test-Path -LiteralPath $zipMongo)) {
        Invoke-WebRequest 'https://fastdl.mongodb.org/windows/mongodb-windows-x86_64-8.0.32.zip' -OutFile $zipMongo
    }
    if ((Get-FileHash -LiteralPath $zipMongo -Algorithm SHA256).Hash -ne '5A0675FDEC49B544CD5D374AD50A9A594CF5B3278B5ACE658AA51CBB4DF17177') {
        throw 'Checksum do ZIP MongoDB inválido. O arquivo não será executado.'
    }
    Expand-Archive -LiteralPath $zipMongo -DestinationPath $cacheMongo -Force
}

$pastaTemporaria = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
$dadosTeste = Join-Path $pastaTemporaria ('farmacia-manager-mongo-test-' + [guid]::NewGuid().ToString('N'))
$mongoProcesso = $null
$uriTeste = 'mongodb://127.0.0.1:55433/?replicaSet=farmacia-teste'

try {
    New-Item -ItemType Directory -Path $dadosTeste | Out-Null
    $mongoProcesso = Start-Process -FilePath $mongoTeste -ArgumentList "--dbpath `"$dadosTeste`" --logpath `"$dadosTeste\servidor.log`" --port 55433 --bind_ip 127.0.0.1 --replSet farmacia-teste" -WindowStyle Hidden -PassThru
    $limiteInicio = [DateTime]::UtcNow.AddSeconds(120)
    do {
        if ($mongoProcesso.HasExited) { throw 'O MongoDB descartável encerrou durante a inicialização.' }
        $clienteTeste = [Net.Sockets.TcpClient]::new()
        try { $clienteTeste.Connect('127.0.0.1', 55433); $conexaoTeste = $true }
        catch { $conexaoTeste = $false }
        finally { $clienteTeste.Dispose() }
        if (-not $conexaoTeste) { Start-Sleep -Milliseconds 200 }
    } until ($conexaoTeste -or [DateTime]::UtcNow -gt $limiteInicio)
    if (-not $conexaoTeste) { throw 'O MongoDB descartável não abriu a porta 55433.' }
    $inicioRegistrado = Get-Content -LiteralPath "$dadosTeste\servidor.log" | ForEach-Object { $_ | ConvertFrom-Json } |
        Where-Object msg -eq 'MongoDB starting' | Select-Object -First 1
    if ($inicioRegistrado.attr.pid -ne $mongoProcesso.Id -or $inicioRegistrado.attr.port -ne 55433) {
        throw 'O log não confirma o processo MongoDB exclusivo de teste.'
    }

    & .\mvnw.cmd -q clean test-compile dependency:build-classpath '-Dmdep.outputFile=target/test-classpath.txt' '-Dmdep.includeScope=test'
    if ($LASTEXITCODE -ne 0) { throw 'A compilação dos testes Java falhou.' }
    $classpathTeste = (Join-Path $PSScriptRoot 'target/test-classes') + ';' + (Join-Path $PSScriptRoot 'target/classes') + ';' + (Get-Content -Raw -LiteralPath 'target/test-classpath.txt').Trim()
    & "$env:JAVA_HOME\bin\java.exe" -cp $classpathTeste com.farmacia.farmacia.MongoReplicaSetTeste $uriTeste
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível inicializar o replica set descartável.' }
    & .\mvnw.cmd -q "-Dfarmacia.test.mongo-uri=$uriTeste" test
    if ($LASTEXITCODE -ne 0) { throw 'Os testes Java falharam. Consulte target/surefire-reports.' }
} catch {
    if (Test-Path -LiteralPath "$dadosTeste\servidor.log") {
        Copy-Item -LiteralPath "$dadosTeste\servidor.log" -Destination (Join-Path $pastaTemporaria 'farmacia-mongo-servidor-ultimo.log') -Force
        Get-Content -LiteralPath "$dadosTeste\servidor.log" -Tail 12
    }
    throw
} finally {
    if ($mongoProcesso -and -not $mongoProcesso.HasExited) {
        # Encerrar somente o processo iniciado por esta execução; nunca procurar mongod por nome.
        $processoAtual = Get-Process -Id $mongoProcesso.Id -ErrorAction SilentlyContinue
        if ($processoAtual -and $processoAtual.StartTime -eq $mongoProcesso.StartTime -and $processoAtual.Path -eq $mongoTeste) {
            Stop-Process -InputObject $processoAtual
            $mongoProcesso.WaitForExit(15000) | Out-Null
        }
    }
    if (Test-Path -LiteralPath $dadosTeste) {
        $alvoLimpeza = (Resolve-Path -LiteralPath $dadosTeste).Path
        if (-not $alvoLimpeza.StartsWith($pastaTemporaria, [StringComparison]::OrdinalIgnoreCase) -or
            (Split-Path -Leaf $alvoLimpeza) -notmatch '^farmacia-manager-mongo-test-[0-9a-f]{32}$' -or
            ($mongoProcesso -and -not $mongoProcesso.HasExited)) {
            throw 'Limpeza interrompida: caminho inesperado ou MongoDB ainda em execução.'
        }
        Remove-Item -LiteralPath $alvoLimpeza -Recurse -Force
    }
}

Write-Output 'Testes concluídos. MongoDB descartável encerrado e removido.'
