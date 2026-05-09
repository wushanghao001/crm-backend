$headers = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbjIiLCJpYXQiOjE3NzgxMjEwMDcsImV4cCI6MTc3ODIwNzQwN30.UxzhrzBKLyd5HaTytLRU8bp7KWu3Ps1BxECI1G9hr48"
}

try {
    Write-Host "=== 测试客户列表 API ==="
    $response = Invoke-RestMethod -Uri "http://localhost:8888/api/customers?page=1" -Method Get -Headers $headers
    Write-Host "客户列表成功: 共 $($response.data.total) 条记录"
    
    Write-Host "`n=== 测试联系人 API ==="
    $response = Invoke-RestMethod -Uri "http://localhost:8888/api/contacts?page=1" -Method Get -Headers $headers
    Write-Host "联系人列表成功: 共 $($response.data.total) 条记录"
    
    Write-Host "`n=== 测试订单 API ==="
    $response = Invoke-RestMethod -Uri "http://localhost:8888/api/orders?page=1" -Method Get -Headers $headers
    Write-Host "订单列表成功: 共 $($response.data.total) 条记录"
    
    Write-Host "`n✅ 所有API测试通过！"
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