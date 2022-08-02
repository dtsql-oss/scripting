$kotlinPath = "kotlin"
$java18Dir = "~/.jdks/azul-18.0.2.1"
$logsFolder = "./evaluation-logs"
$scriptLogFile = "./evaluate.log"
$executionsPerFile = 10
$inputFiles = @(
    "./data/00_evaluation_86400.csv",
    "./data/01_evaluation_43200.csv",
    "./data/02_evaluation_21600.csv",
    "./data/03_evaluation_10800.csv",
    "./data/04_evaluation_5400.csv",
    "./data/05_evaluation_2700.csv",
    "./data/06_evaluation_1350.csv",
    "./data/07_evaluation_675.csv",
    "./data/08_evaluation_338.csv",
    "./data/09_evaluation_169.csv",
    "./data/10_evaluation_85.csv",
    "./data/11_evaluation_43.csv",
    "./data/12_evaluation_21.csv"
)
$scripts = @(
    "./01-global-aggregates.main.kts",
    "./02-local-aggregates.main.kts",
    "./03-temporal-aggregates.main.kts",
    "./04-numerical-integral.main.kts",
    "./05-threshold-filters.main.kts",
    "./06-temporal-filters.main.kts",
    "./07-threshold-events.main.kts",
    "./08-deviation-events.main.kts",
    "./09-constant-events.main.kts",
    "./10-monotonic-events.main.kts",
    "./11-duration-constraints-for-events.main.kts",
    "./12-binary-event-sequences.main.kts",
    "./13-time-tolerance-for-event-sequences.main.kts",
    "./14-nary-event-sequences.main.kts"
)

$oldLogFolders = @((Get-ChildItem -Path "." -Filter "evaluation-logs*").Fullname)
Write-Host "Found old log folders:"
Write-Host ($oldLogFolders -join "`n")
Write-Host ""
if ($oldLogFolders.Count -gt 0) {
    for ($i = 0; $i -lt $oldLogFolders.Count; $i++) {
        $currentLogsFolder = $oldLogFolders[$i]
        Write-Host "Deleting" $currentLogsFolder
        Remove-Item -Recurse -Force $currentLogsFolder
    }
}

if (Test-Path -Path $scriptLogFile) {
    Write-Host "Deleting old log file" $scriptLogFile
    Remove-Item -Recurse -Force $scriptLogFile
}

Write-Host ""
Write-Host "Setting JAVA_HOME and Java in PATH to" $java18Dir
$env:JAVA_HOME = $java18Dir
$env:Path = $env:JAVA_HOME + ";" + $env:Path
Write-Host ""

Write-Host "Input Files:"
Write-Host ($inputFiles -Join "`n")
Write-Host ""
Write-Host "Query Scripts:"
Write-Host ($scripts -Join "`n")
Write-Host "=========================================="
Write-Host ""

for ($k = 0; $k -lt $inputFiles.Count; $k++) {
    $currentInputData = $inputFiles[$k]
    Write-Host "##############################################################################"
    Write-Host "## Current Input Data:" $currentInputData "##"
    Write-Host "##############################################################################"
    Write-Host (Get-Date)

    for ($i = 0; $i -lt $scripts.Count; $i++) {
        $currentFile = $scripts[$i]
        Write-Host "Current Script:" $currentFile
        Write-Host (Get-Date)

        for ($j = 0; $j -lt $executionsPerFile; $j++) {
            Write-Host ($j + 1) "/" $executionsPerFile
            Write-Host (Get-Date)
            & $kotlinPath "-J--enable-preview" "-J-Xmx2g" $currentFile $currentInputData
        }

        Write-Host "================================="
    }

    Rename-Item -Path $logsFolder -NewName ($logsFolder + "_" + $k)
}

Write-Host "Done."
Write-Host (Get-Date)
