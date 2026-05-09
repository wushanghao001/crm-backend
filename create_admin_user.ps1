$headers = @{
    "Content-Type" = "application/json"
}

$registerData = @{
    username = "superadmin"
    email = "superadmin@example.com"
    password = "super123"
} | ConvertTo-Json

try {
    Write-Host "=== 注册超级管理员用户 ==="
    $response = Invoke-RestMethod -Uri "http://localhost:8888/api/auth/register" -Method Post -Headers $headers -Body $registerData
    Write-Host "✅ 超级管理员用户创建成功!"
    Write-Host "用户名: superadmin"
    Write-Host "密码: super123"
} catch {
    Write-Host "Error:"
    Write-Host $_.Exception.Message
    if ($_.Exception.Response) {
        $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        $reader.Close()
        Write-Host "Response Body: " $responseBody
    }
}