$headers = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbjIiLCJpYXQiOjE3NzgxMjEwMDcsImV4cCI6MTc3ODIwNzQwN30.UxzhrzBKLyd5HaTytLRU8bp7KWu3Ps1BxECI1G9hr48"
}

try {
    Write-Host "=== 获取当前用户信息 ==="
    $response = Invoke-RestMethod -Uri "http://localhost:8888/api/auth/me" -Method Get -Headers $headers
    Write-Host "用户名: " $response.data.user.username
    Write-Host "角色: " $response.data.user.role
    Write-Host "权限列表:"
    $response.data.user.permissions | ForEach-Object { Write-Host "  - $_" }
} catch {
    Write-Host "Error:"
    Write-Host $_.Exception.Message
}