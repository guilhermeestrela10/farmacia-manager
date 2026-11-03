$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath "$env:JAVA_HOME\bin\javac.exe")) {
    $jdkTeste = Get-ChildItem 'C:\Program Files\Java' -Directory |
        Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\javac.exe') } |
        Sort-Object Name -Descending | Select-Object -First 1
    if (-not $jdkTeste) { throw 'JDK não encontrado. Configure JAVA_HOME com um JDK 21 ou superior.' }
    $env:JAVA_HOME = $jdkTeste.FullName
}

node --test src/test/js/estoque-medicamentos.test.cjs src/test/js/vendas.test.cjs
if ($LASTEXITCODE -ne 0) { throw 'Os testes JavaScript falharam.' }

$pgBinTeste = 'C:\Program Files\PostgreSQL\18\bin'
if (-not (Test-Path -LiteralPath "$pgBinTeste\initdb.exe")) { throw 'PostgreSQL 18 não encontrado em Program Files.' }
if (Get-NetTCPConnection -LocalPort 55432 -State Listen -ErrorAction SilentlyContinue) {
    throw 'A porta 55432 está ocupada. Nenhum banco existente será reutilizado.'
}

$pastaTemporaria = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
$dadosTeste = Join-Path $pastaTemporaria ('farmacia-manager-test-' + [guid]::NewGuid().ToString('N'))
$pgCtlTeste = Join-Path $pgBinTeste 'pg_ctl.exe'

try {
    & "$pgBinTeste\initdb.exe" -D $dadosTeste -U farmacia_teste -A trust --encoding=UTF8 --locale=C --locale-provider=icu --icu-locale=pt-BR
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível criar o PostgreSQL descartável.' }
    $pgInicioTeste = Start-Process -FilePath $pgCtlTeste -ArgumentList "-D `"$dadosTeste`" -l `"$dadosTeste\servidor.log`" -o `"-p 55432 -h 127.0.0.1`" -t 15 -w start" -WindowStyle Hidden -PassThru
    $pgInicioTeste.WaitForExit()
    if ($pgInicioTeste.ExitCode -ne 0) { throw 'O PostgreSQL descartável não iniciou.' }

    & .\mvnw.cmd -q clean '-Dfarmacia.test.url=jdbc:postgresql://127.0.0.1:55432/postgres' test
    if ($LASTEXITCODE -ne 0) { throw 'Os testes Java falharam. Consulte target/surefire-reports.' }
} finally {
    if (Test-Path -LiteralPath "$dadosTeste\postmaster.pid") {
        $pgFimTeste = Start-Process -FilePath $pgCtlTeste -ArgumentList "-D `"$dadosTeste`" -m fast -t 15 -w stop" -WindowStyle Hidden -PassThru
        $pgFimTeste.WaitForExit()
    }
    if (Test-Path -LiteralPath $dadosTeste) {
        $alvoLimpeza = (Resolve-Path -LiteralPath $dadosTeste).Path
        if (-not $alvoLimpeza.StartsWith($pastaTemporaria, [StringComparison]::OrdinalIgnoreCase) -or
            (Split-Path -Leaf $alvoLimpeza) -notlike 'farmacia-manager-test-*' -or
            (Test-Path -LiteralPath "$alvoLimpeza\postmaster.pid")) {
            throw 'Limpeza interrompida: caminho inesperado ou PostgreSQL ainda em execução.'
        }
        Remove-Item -LiteralPath $alvoLimpeza -Recurse -Force
    }
}

Write-Output 'Testes concluídos. PostgreSQL descartável encerrado e removido.'
