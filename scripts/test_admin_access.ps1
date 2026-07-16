$token = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0aW5vbWF0aW1iZUBnbWFpbC5jb20iLCJpYXQiOjE3ODEwMTMzMzEsImV4cCI6MTc4MTAxNjkzMX0.GrlPaEaJloRHRH9jWeBK2b0QbUuUJgFIqODzK_yUJmU'
try {
  $res = Invoke-RestMethod -Uri 'http://localhost:8080/api/users' -Method Get -Headers @{ Authorization = "Bearer $token" }
  Write-Output 'ADMIN_ACCESS_OK'
  $res | ConvertTo-Json -Compress
} catch {
  Write-Output 'ADMIN_ACCESS_FAILED:'
  if ($_.Exception.Response) {
    $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    Write-Output $sr.ReadToEnd()
  } else {
    Write-Output $_.Exception.Message
  }
}
