# Getting user followers

This method returns the list of users who follow the current user.

A follower is a user who follows the current user's updates. A subscription can be created directly or as a side effect of sending a friendship offer.

## HTTP request

```http
GET /api/subscribes/followers?userId=2
Accept: application/json
```

## Query parameters

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `userId` | `number` | Yes | Identifier of the user whose followers list should be returned. |

## Successful response

If followers are found, the API returns `200 OK` and the list of users.

```http
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
[
  {
    "id": 1,
    "username": "alice",
    "displayName": "Alice"
  },
  {
    "id": 3,
    "username": "maria",
    "displayName": "Maria"
  }
]
```

If there are no followers, the API returns an empty list.

```json
[]
```

### Response fields

| Field | Type | Description |
| --- | --- | --- |
| `id` | `number` | Follower user identifier. |
| `username` | `string` | Unique username. |
| `displayName` | `string` | User display name. |

## Processing rules

- The method returns users who are subscribed to the user from the `userId` parameter.
- If Alice follows Boris, then Alice will be a follower for Boris.
- Followers are not necessarily the user's friends.
- If there are no followers, an empty list is returned instead of an error.

## Idempotency

The method only reads data and does not change the system state.

- A repeated call does not create or delete subscriptions.
- A repeated call does not create or delete friendship.
- If the data has not changed, a repeated call returns the same followers list.

## Possible errors

| HTTP status | Case |
| --- | --- |
| `400 Bad Request` | Invalid or missing `userId` parameter. |
| `404 Not Found` | User was not found. |
