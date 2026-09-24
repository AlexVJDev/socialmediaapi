# Accepting a friendship offer

This method accepts an existing friendship offer that is in the `PENDING` state.

When the offer is accepted successfully:

- the request state changes to `ACCEPTED`;
- friendship is created between the users;
- the offer recipient subscribes to the initiator if such a subscription does not already exist;
- as a result, both users have a mutual subscription.

## HTTP request

```http
POST /api/offer-friendships/accept
Content-Type: application/json
```

## Request body

The request body contains identifiers of the initiating user and the recipient user.

```json
{
  "fromUserId": 1,
  "toUserId": 2
}
```

### Request fields

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `fromUserId` | `number` | Yes | Identifier of the user who sent the friendship offer. |
| `toUserId` | `number` | Yes | Identifier of the user who accepts the friendship offer. |

## Successful response

If the offer is accepted successfully, the API returns `200 OK` and the updated request data.

```http
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "id": 10,
  "fromUserId": 1,
  "toUserId": 2,
  "status": "ACCEPTED",
  "createdAt": "2026-09-22T10:15:00Z",
  "acceptedAt": "2026-09-22T10:20:00Z"
}
```

### Response fields

| Field | Type | Description |
| --- | --- | --- |
| `id` | `number` | Friendship offer identifier. |
| `fromUserId` | `number` | Identifier of the initiating user. |
| `toUserId` | `number` | Identifier of the user who accepted the offer. |
| `status` | `string` | Current offer state. After successful acceptance — `ACCEPTED`. |
| `createdAt` | `string` | Offer creation date and time in ISO 8601 format. |
| `acceptedAt` | `string` | Offer acceptance date and time in ISO 8601 format. |

## Processing rules

- Only an existing friendship offer can be accepted.
- The offer must be in the `PENDING` state.
- After acceptance, the offer state changes to `ACCEPTED`.
- Acceptance creates a friendship link between the initiator and the recipient.
- Acceptance creates a reverse subscription from the recipient to the initiator if it does not already exist.
- Re-accepting an already accepted offer must not create duplicate subscriptions or friendship links.
- A rejected offer cannot be accepted without a separate business rule for sending a new offer.

## Idempotency

A repeated HTTP request is possible when the client does not receive a response due to a network error.

- If the offer is already in the `ACCEPTED` state, the repeated call returns the already reached result and does not create a second friendship.
- The repeated call does not create a duplicate reverse subscription from the recipient to the initiator.
- If the offer is in the `REJECTED` state, a repeated attempt to accept it ends with a conflict.
- If the offer is not found, the repeated call returns a missing request error.

## Possible errors

| HTTP status | Case |
| --- | --- |
| `400 Bad Request` | Invalid request body or missing required fields. |
| `404 Not Found` | Friendship offer was not found. |
| `409 Conflict` | The offer cannot be accepted because of its current state, for example it has already been rejected. |
