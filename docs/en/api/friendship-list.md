# Getting the friends list

This method returns the user's friends list.

A friend is a user with whom a bidirectional friendship link has already been created. Such a link appears after a friendship offer is accepted and the request moves to the `ACCEPTED` state.

## HTTP request

```http
GET /api/friendships?userId=1
Accept: application/json
```

## Query parameters

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `userId` | `number` | Yes | Identifier of the user whose friends list should be returned. |

## Successful response

If friends are found, the API returns `200 OK` and the list of users.

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
    "id": 3,
    "username": "maria",
    "displayName": "Maria"
  }
]
```

If there are no friends, the API returns an empty list.

```json
[]
```

### Response fields

| Field | Type | Description |
| --- | --- | --- |
| `id` | `number` | Friend user identifier. |
| `username` | `string` | Unique username. |
| `displayName` | `string` | User display name. |

## Processing rules

- The method returns only users with whom the current user has a friendship link.
- Requests in the `PENDING` state are not considered friendship and are not included in the friends list.
- Requests in the `REJECTED` state are not considered friendship and are not included in the friends list.
- If there are no friends, an empty list is returned instead of an error.

## Idempotency

The method only reads data and does not change the system state.

- A repeated call does not create or delete friendship.
- A repeated call does not create subscriptions.
- If the data has not changed, a repeated call returns the same friends list.

## Possible errors

| HTTP status | Case |
| --- | --- |
| `400 Bad Request` | Invalid or missing `userId` parameter. |
| `404 Not Found` | User was not found. |
