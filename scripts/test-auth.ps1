$body = @{ username='admin1'; password='Senha123!'; fullName='Administrador' } | ConvertTo-Json
try {
    $res = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/register' -Method Post -ContentType 'application/json' -Body $body
    Write-Output 'OK'
    $res | ConvertTo-Json
} catch [Exception] {
    $err = $_.Exception
    if ($err.Response -ne $null) {
        $sr = New-Object System.IO.StreamReader($err.Response.GetResponseStream())
        Write-Output $err.Response.StatusCode.Value__
        Write-Output $err.Response.StatusDescription
        Write-Output $sr.ReadToEnd()
    } else {
        Write-Output $err.Message
    }
}
