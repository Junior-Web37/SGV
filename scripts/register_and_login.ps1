$body = @{username='tinomatimbe@gmail.com'; password='mmmmmm'; fullName='Tino Matimbe'}
try {
  Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/register' -Method Post -ContentType 'application/json' -Body ($body | ConvertTo-Json -Compress) -ErrorAction Stop
  Write-Output 'REGISTER_OK'
} catch {
  Write-Output 'REGISTER_FAILED:'
  if ($_.Exception.Response) {
    $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    Write-Output $sr.ReadToEnd()
  } else {
    Write-Output $_.Exception.Message
  }
}
try {
  $login = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body ((@{username=$body.username; password=$body.password}) | ConvertTo-Json -Compress) -ErrorAction Stop
  Write-Output ('USERNAME:' + $body.username)
  Write-Output ('TOKEN:' + $login.token)
} catch {
  Write-Output 'LOGIN_FAILED:'
  if ($_.Exception.Response) {
    $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    Write-Output $sr.ReadToEnd()
  } else {
    Write-Output $_.Exception.Message
  }
}
