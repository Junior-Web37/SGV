$token = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0aW5vbWF0aW1iZUBnbWFpbC5jb20iLCJpYXQiOjE3ODEwMTE4ODEsImV4cCI6MTc4MTAxNTQ4MX0.XLD_9G_l_G80387ho7IDjj3yKWLoCHpwNmiCrEQWgD4'
try {
  $res = Invoke-RestMethod -Uri 'http://localhost:8080/api/users/me' -Method Get -Headers @{ Authorization = "Bearer $token" } -ErrorAction Stop
  $res | ConvertTo-Json -Compress
} catch {
  Write-Output 'REQUEST_FAILED'
  if ($_.Exception.Response) {
    $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    Write-Output $sr.ReadToEnd()
  } else {
    Write-Output $_.Exception.Message
  }
}
