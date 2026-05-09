$headers = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbjIiLCJpYXQiOjE3NzgxMjEwMDcsImV4cCI6MTc3ODIwNzQwN30.UxzhrzBKLyd5HaTytLRU8bp7KWu3Ps1BxECI1G9hr48"
}

$updateData = @{
    username = "admin2"
    role = "admin"
    permissions = "customer:view,customer:add,customer:edit,customer:delete,contact:view,contact:add,contact:edit,contact:delete,interaction:view,interaction:add,interaction:edit,interaction:delete,order:view,order:add,order:edit,order:delete,churn:view,opportunity:view,service:view,product:view,statistics:view,user:view,role:view,log:view"
} | ConvertTo-Json

try {
    Write-Host "=== 更新用户权限 ==="
    $response = Invoke-RestMethod -Uri "http://localhost:8888/api/users/2" -Method Put -Headers $headers -Body $updateData
    Write-Host "更新成功!"
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