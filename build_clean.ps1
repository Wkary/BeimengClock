# Clean-room build for beimeng clock. ASCII only on purpose:
# the Windows PowerShell 5.1 parser chokes on UTF-8 Chinese in this host.
$ErrorActionPreference = 'Stop'

$ws         = 'C:\bm_clean'
$jdk        = 'C:\bm_tools\jdk-17.0.20.1+1'
$java       = $jdk + '\bin\java.exe'
$javac      = $jdk + '\bin\javac.exe'
$jar        = $jdk + '\bin\jar.exe'
$androidJar = 'C:\Tools\pl\android.jar'
$r8jar      = 'C:\Users\admin\Doubao\chats\2026-10-18\new-chat\LauncherLayoutBackup-main\_build\libs\r8.jar'
$bin        = $ws + '\bin'
$aapt2      = $bin + '\aapt2_64.exe'
$signer     = 'C:\bm_tools\uber-apk-signer.jar'
$outDir     = $ws + '\out'
$build      = $ws + '\build'
$q          = [char]34

foreach ($p in @($java, $javac, $androidJar, $r8jar, $aapt2, $signer)) {
    if (-not (Test-Path $p)) { throw ('missing tool: ' + $p) }
}

Write-Host '[0/6] clean build dir'
if (Test-Path $build) { Remove-Item -Recurse -Force $build }
New-Item -ItemType Directory -Force $build            | Out-Null
New-Item -ItemType Directory -Force ($build + '\gen')     | Out-Null
New-Item -ItemType Directory -Force ($build + '\classes') | Out-Null
New-Item -ItemType Directory -Force ($build + '\dex')     | Out-Null
New-Item -ItemType Directory -Force $outDir           | Out-Null

Write-Host '[1/6] aapt2 compile'
$flat = $build + '\res.flata'
& $aapt2 compile --dir ($ws + '\res') -o $flat
if ($LASTEXITCODE -ne 0) { throw ('aapt2 compile failed ' + $LASTEXITCODE) }
Write-Host ('      res.flata ' + (Get-Item $flat).Length + ' bytes')

Write-Host '[2/6] aapt2 link'
$baseApk = $build + '\base.apk'
& $aapt2 link -o $baseApk -I $androidJar --manifest ($ws + '\AndroidManifest.xml') --java ($build + '\gen') --auto-add-overlay $flat
if ($LASTEXITCODE -ne 0) { throw ('aapt2 link failed ' + $LASTEXITCODE) }

$rjava = Get-ChildItem -Recurse -File ($build + '\gen') -Filter 'R.java' | Select-Object -First 1
if (-not $rjava) { throw 'R.java not generated' }
Write-Host ('      R.java ' + $rjava.FullName)

Write-Host '[3/6] javac'
$sources = @(Get-ChildItem -Recurse -File ($ws + '\src') -Filter '*.java' | ForEach-Object { $_.FullName })
$sources += $rjava.FullName
Write-Host ('      java files ' + $sources.Count)

$argfile = $build + '\javac.args'
$cp   = $q + $androidJar.Replace('\','/') + $q
$outc = $q + ($build + '\classes').Replace('\','/') + $q
$lines = @('-encoding','UTF-8','-source','8','-target','8','-nowarn','-Xlint:-options')
$lines += @('-classpath', $cp)
$lines += @('-d', $outc)
foreach ($s in $sources) { $lines += ($q + $s.Replace('\','/') + $q) }
[IO.File]::WriteAllLines($argfile, $lines, (New-Object System.Text.UTF8Encoding($false)))

$oldEap = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
$jcOut = & $javac ('@' + $argfile) 2>&1
$ErrorActionPreference = $oldEap
$jcCode = $LASTEXITCODE
$jcErr = ($jcOut | Where-Object { $_ -match 'error:|错误:' } | Measure-Object).Count
if ($jcCode -ne 0 -and $jcErr -gt 0) { $jcOut | Select-Object -Last 20 | ForEach-Object { Write-Host $_ }; throw ('javac failed ' + $jcCode) }
if ($LASTEXITCODE -ne 0) { throw ('javac failed ' + $LASTEXITCODE) }
$clsCount = @(Get-ChildItem -Recurse -File ($build + '\classes') -Filter '*.class').Count
Write-Host ('      .class files ' + $clsCount)

Write-Host '[4/6] d8'
$allClass = @(Get-ChildItem -Recurse -File ($build + '\classes') -Filter '*.class' | ForEach-Object { $_.FullName })
$d8out = $build + '\dex'
& $java '-cp' $r8jar 'com.android.tools.r8.D8' '--release' '--min-api' '24' '--lib' $androidJar '--output' $d8out @allClass
if ($LASTEXITCODE -ne 0) { throw ('d8 failed ' + $LASTEXITCODE) }

$dex = $d8out + '\classes.dex'
if (-not (Test-Path $dex)) { throw 'classes.dex not produced' }
Write-Host ('      classes.dex ' + (Get-Item $dex).Length + ' bytes')

Write-Host '[5/6] assemble apk'
$unsigned = $build + '\unsigned.apk'
Copy-Item $baseApk $unsigned -Force
Push-Location $d8out
try { & $jar 'uf' $unsigned 'classes.dex' } finally { Pop-Location }
if ($LASTEXITCODE -ne 0) { throw ('jar update failed ' + $LASTEXITCODE) }
Add-Type -AssemblyName System.IO.Compression.FileSystem

Write-Host '[6/6] zipalign + sign'
$signedDir = $build + '\signed'
New-Item -ItemType Directory -Force $signedDir | Out-Null
& $java '-jar' $signer '-a' $unsigned '-o' $signedDir '--allowResign'
if ($LASTEXITCODE -ne 0) { throw ('signing failed ' + $LASTEXITCODE) }

$signed = Get-ChildItem $signedDir -Filter '*Signed.apk' | Select-Object -First 1
if (-not $signed) { throw 'no signed apk' }

Add-Type -AssemblyName System.IO.Compression.FileSystem
$z = [System.IO.Compression.ZipFile]::OpenRead($signed.FullName)
$dexCount = @($z.Entries | Where-Object { $_.FullName -like '*.dex' }).Count
$hasArsc = [bool]$z.GetEntry('resources.arsc')
$hasMan  = [bool]$z.GetEntry('AndroidManifest.xml')
$resN    = @($z.Entries | Where-Object { $_.FullName -like 'res/*' }).Count
$z.Dispose()

Write-Host ''
Write-Host '========== VERIFY =========='
Write-Host ('dex count        : ' + $dexCount)
Write-Host ('resources.arsc   : ' + $hasArsc)
Write-Host ('AndroidManifest  : ' + $hasMan)
Write-Host ('res entries      : ' + $resN)
Write-Host ('size             : ' + $signed.Length + ' bytes')

if ($dexCount -ne 1) { throw ('unexpected dex count ' + $dexCount) }
if (-not $hasArsc) { throw 'no resources.arsc' }
if (-not $hasMan)  { throw 'no AndroidManifest' }

Copy-Item $signed.FullName ($outDir + '\beimeng_clean.apk') -Force
Write-Host ('OUTPUT ' + $outDir + '\beimeng_clean.apk')
