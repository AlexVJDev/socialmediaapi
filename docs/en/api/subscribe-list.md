# Getting user subscriptions

This method returns the list of users whom the current user follows.

A subscription means that the user follows another user's updates. A subscription can be created directly or automatically when a friendship offer is sent.

## HTTP request

```http
GET /api/subscribes?userId=1
Accept: application/json
```

## Query parameters

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `userId` | `number` | Yes | Identifier of the user whose subscriptions list should be returned. |

## Successful response

If subscriptions are found, the API returns `200 OK` and the list of users.

```http
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
[
  {
    "id": 2,
    "username": "boris",
    "displayName": "Boris"
  },
  {
    "id": 4,
    "username": "ivan",
    "displayName": "Ivan"
  }
]
```

If there are no subscriptions, the API returns an empty list.

```json
[]
```

### Response fields

| Field | Type | Description |
| --- | --- | --- |
| `id` | `number` | Identifier of the user being followed. |
| `username` | `string` | Unique username. |
| `displayName` | `string` | User display name. |

## Processing rules

- The method returns users whom the user from the `userId` parameter follows.
- If Alice follows Boris, then Boris will be displayed in Alice's subscriptions list.
- A subscription does not necessarily mean friendship between users.
- If there are no subscriptions, an empty list is returned instead of an error.

## Idempotency

The method only reads data and does not change the system state.

- A repeated call does not create or delete subscriptions.
- A repeated call does not create or delete friendship.
- If the data has not changed, a repeated call returns the same subscriptions list.

## Possible errors

| HTTP status | Case |
| --- | --- |
| `400 Bad Request` | Invalid or missing `userId` parameter. |
| `404 Not Found` | User was not found. |
