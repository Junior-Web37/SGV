$body = @{ username = 'tinomatimbe@gmail.com'; password = 'mmmmmm' }
try {
  $res = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body ($body | ConvertTo-Json)
  Write-Output 'SUCCESS'
  $res | ConvertTo-Json -Compress
} catch {
  Write-Output 'FAILED:'
  if ($_.Exception.Response) {
    $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    Write-Output $sr.ReadToEnd()
  } else {
    Write-Output $_.Exception.Message
  }
}
