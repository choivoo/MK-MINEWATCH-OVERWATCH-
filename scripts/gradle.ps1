# 한글/비ASCII 경로에서는 Gradle 테스트 워커가 클래스를 찾지 못한다(JVM 인코딩 문제).
# 이 스크립트는 ASCII 경로 정션(C:\mwjunction)을 만들어 그 경로에서 gradle 을 실행한다.
# 사용: powershell -File scripts\gradle.ps1 build   (또는 test, runClient ...)
param([Parameter(ValueFromRemainingArguments = $true)] [string[]] $GradleArgs)
$repo = Split-Path -Parent $PSScriptRoot
$link = 'C:\mwjunction'
if (-not (Test-Path $link)) { cmd /c mklink /J $link "$repo" | Out-Null }
Set-Location $link
& .\gradlew.bat --console=plain @GradleArgs
