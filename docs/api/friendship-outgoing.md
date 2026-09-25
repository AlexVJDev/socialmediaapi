# Getting outgoing friendship offers

This method returns the list of friendship offers that the user has sent to other users.

An offer is considered outgoing when the user is the request initiator. This list can be used to show the user's active outgoing requests.

## HTTP request

```http
GET /api/offer-friendships/outgoing?userId=1
Accept: application/json
```

## Query parameters

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `userId` | `number` | Yes | Identifier of the user whose sent friendship offers should be returned. |

## Successful response

If outgoing offers are found, the API returns `200 OK` and the list of requests.

```http
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
[
  {
    "id": 10,
    "fromUserId": 1,
    "toUserId": 2,
    "status": "PENDING",
    "createdAt": "2026-09-22T10:15:00Z"
  },
  {
    "id": 12,
    "fromUserId": 1,
    "toUserId": 4,
    "status": "PENDING",
    "createdAt": "2026-09-22T10:30:00Z"
  }
]
```

If there are no outgoing offers, the API returns an empty list.

```json
[]
```

### Response fields

| Field | Type | Description |
| --- | --- | --- |
| `id` | `number` | Friendship offer identifier. |
| `fromUserId` | `number` | Identifier of the user who sent the friendship offer. |
| `toUserId` | `number` | Identifier of the user who received the friendship offer. |
| `status` | `string` | Current offer state. Active outgoing requests usually use `PENDING`. |
| `createdAt` | `string` | Offer creation date and time in ISO 8601 format. |

## Processing rules

- The method returns offers where the user is specified as the request initiator.
- By default, the list should contain active outgoing offers in the `PENDING` state.
- Accepted offers in the `ACCEPTED` state have already created friendship and are usually not displayed as active outgoing requests.
- Rejected offers in the `REJECTED` state have already been processed by the recipient and are usually not displayed as active outgoing requests.
- If there are no outgoing offers, an empty list is returned instead of an error.

## Idempotency

The method only reads data and does not change the system state.

- A repeated call does not create new friendship offers.
- A repeated call does not create subscriptions or friendship links.
- If the data has not changed, a repeated call returns the same list of outgoing offers.

## Possible errors

| HTTP status | Case |
| --- | --- |
| `400 Bad Request` | Invalid or missing `userId` parameter. |
| `404 Not Found` | User was not found. |
