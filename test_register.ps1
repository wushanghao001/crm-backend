$body = @{
    username = "admin2"
    email = "admin2@example.com"
    password = "admin123"
} | ConvertTo-Json

$headers = @{
    "Content-Type" = "application/json"
}

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8888/api/auth/register" -Method Post -Body $body -Headers $headers
    Write-Host "Response:"
    Write-Host ($response | ConvertTo-Json -Depth 10)

    Write-Host "`nNow trying to login with new user:"
    $loginBody = @{
        username = "admin2"
        password = "admin123"
    } | ConvertTo-Json

    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8888/api/auth/login" -Method Post -Body $loginBody -Headers $headers
    Write-Host "Login Response:"
    Write-Host ($loginResponse | ConvertTo-Json -Depth 10)
} catch {
    Write-Host "Error:"
    Write-Host $_.Exception.Message
    if ($_.Exception.Response) {
        Write-Host "Status Code: " $_.Exception.Response.StatusCode.value__
        $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        $reader.Close()
        Write-Host "Response Body: " $responseBody
    }
}