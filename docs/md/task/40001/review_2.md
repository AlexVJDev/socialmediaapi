Ошибки при первом прогоне:
Авторидовался как пользователь из Readme: 0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01

Запросы и ответы который получил: 

============= 
curl -X 'POST' \
'http://localhost:8080/api/v1/friendship-offer/create' \
-H 'accept: application/problem+json' \
-H 'Content-Type: application/json' \
-d '{
"fromUserId": "0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01",
"toUserId": "5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22"
}'

401
Undocumented
Error: response status is 401

Response body
Download
{
"status": 401,
"detail": "Пользователь не определён",
"code": "UNAUTHORIZED"
}
Response headers
connection: keep-alive
content-type: application/problem+json
date: Fri,20 Nov 2026 16:59:56 GMT
keep-alive: timeout=60
transfer-encoding: chunked 


=========== 

curl -X 'POST' \
'http://localhost:8080/api/v1/friendship-offer/accept' \
-H 'accept: application/json' \
-H 'Content-Type: application/json' \
-d '{
"offerId": "9c4e1b77-3f2a-4d6c-8b0e-1a5f7d2c3e44",
"actorId": "5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22"
}'


Code	Details
401
Undocumented
Error: response status is 401

Response body
Download
{
"status": 401,
"detail": "Пользователь не определён",
"code": "UNAUTHORIZED"
}
Response headers
connection: keep-alive
content-type: application/problem+json
date: Fri,20 Nov 2026 17:03:24 GMT
keep-alive: timeout=60
transfer-encoding: chunked 


========== 

Curl

curl -X 'POST' \
'http://localhost:8080/api/v1/friendship-offer/reject' \
-H 'accept: application/json' \
-H 'Content-Type: application/json' \
-d '{
"offerId": "9c4e1b77-3f2a-4d6c-8b0e-1a5f7d2c3e44",
"actorId": "5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22"
}'
Request URL
http://localhost:8080/api/v1/friendship-offer/reject
Server response
Code	Details
401
Undocumented
Error: response status is 401

Response body
Download
{
"status": 401,
"detail": "Пользователь не определён",
"code": "UNAUTHORIZED"
}
Response headers
connection: keep-alive
content-type: application/problem+json
date: Fri,20 Nov 2026 17:04:03 GMT
keep-alive: timeout=60
transfer-encoding: chunked 


============== 

