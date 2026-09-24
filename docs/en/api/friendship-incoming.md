# Getting incoming friendship offers

This method returns the list of incoming friendship offers for a user.

An offer is considered incoming when the user is the request recipient. This list is usually used to show requests that the user can accept or reject.

## HTTP request

```http
GET /api/offer-friendships/incoming?userId=2
Accept: application/json
```

## Query parameters

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `userId` | `number` | Yes | Identifier of the user whose incoming friendship offers should be returned. |

## Successful response

If incoming offers are found, the API returns `200 OK` and the list of requests.

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
    "id": 11,
    "fromUserId": 3,
    "toUserId": 2,
    "status": "PENDING",
    "createdAt": "2026-09-22T10:25:00Z"
  }
]
```

If there are no incoming offers, the API returns an empty list.

```json
[]
```

### Response fields

| Field | Type | Description |
| --- | --- | --- |
| `id` | `number` | Friendship offer identifier. |
| `fromUserId` | `number` | Identifier of the user who sent the friendship offer. |
| `toUserId` | `number` | Identifier of the user who received the friendship offer. |
| `status` | `string` | Current offer state. Active incoming requests usually use `PENDING`. |
| `createdAt` | `string` | Offer creation date and time in ISO 8601 format. |

## Processing rules

- The method returns offers where the user is specified as the request recipient.
- By default, the list should contain active incoming offers in the `PENDING` state.
- Accepted offers in the `ACCEPTED` state do not require a user decision and are usually not displayed as incoming.
- Rejected offers in the `REJECTED` state do not require a user decision and are usually not displayed as incoming.
- If there are no incoming offers, an empty list is returned instead of an error.

## Idempotency

The method only reads data and does not change the system state.

- A repeated call does not create, accept, or reject friendship offers.
- A repeated call does not create subscriptions or friendship links.
- If the data has not changed, a repeated call returns the same list of incoming offers.

## Possible errors

| HTTP status | Case |
| --- | --- |
| `400 Bad Request` | Invalid or missing `userId` parameter. |
| `404 Not Found` | User was not found. |
