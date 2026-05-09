Write-Host "=== Testing Login API ==="
$body = '{"username":"admin","password":"admin123"}'
$wc = New-Object System.Net.WebClient
$wc.Headers['Content-Type'] = 'application/json'
$response = $wc.UploadString('http://localhost:8888/api/auth/login', 'POST', $body)
Write-Host $response