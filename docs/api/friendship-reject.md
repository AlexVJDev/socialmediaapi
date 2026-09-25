# Rejecting a friendship offer

This method rejects an existing friendship offer that is in the `PENDING` state.

When the offer is rejected successfully:

- the request state changes to `REJECTED`;
- friendship between the users is not created;
- the reverse subscription from the recipient to the initiator is not created;
- the existing subscription from the initiator to the recipient is preserved.

## HTTP request

```http
POST /api/offer-friendships/reject
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
| `toUserId` | `number` | Yes | Identifier of the user who rejects the friendship offer. |

## Successful response

If the offer is rejected successfully, the API returns `200 OK` and the updated request data.

```http
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "id": 10,
  "fromUserId": 1,
  "toUserId": 2,
  "status": "REJECTED",
  "createdAt": "2026-09-22T10:15:00Z",
  "rejectedAt": "2026-09-22T10:20:00Z"
}
```

### Response fields

| Field | Type | Description |
| --- | --- | --- |
| `id` | `number` | Friendship offer identifier. |
| `fromUserId` | `number` | Identifier of the initiating user. |
| `toUserId` | `number` | Identifier of the user who rejected the offer. |
| `status` | `string` | Current offer state. After successful rejection — `REJECTED`. |
| `createdAt` | `string` | Offer creation date and time in ISO 8601 format. |
| `rejectedAt` | `string` | Offer rejection date and time in ISO 8601 format. |

## Processing rules

- Only an existing friendship offer can be rejected.
- The offer must be in the `PENDING` state.
- After rejection, the offer state changes to `REJECTED`.
- Rejection does not create a friendship link between the initiator and the recipient.
- Rejection does not create a reverse subscription from the recipient to the initiator.
- The initiator's subscription to the recipient, created when the friendship offer was sent, is preserved.
- Re-rejecting an already rejected offer must not change the request state, subscriptions, or friendship.
- An already accepted offer cannot be rejected because friendship has already been created.

## Idempotency

A repeated HTTP request is possible when the client does not receive a response due to a network error.

- If the offer is already in the `REJECTED` state, the repeated call returns the already reached result and does not change data.
- Repeated rejection does not remove the initiator's subscription to the recipient.
- Repeated rejection does not create friendship and does not create a reverse subscription.
- If the offer is in the `ACCEPTED` state, a repeated attempt to reject it ends with a conflict because friendship has already been created.

## Possible errors

| HTTP status | Case |
| --- | --- |
| `400 Bad Request` | Invalid request body or missing required fields. |
| `404 Not Found` | Friendship offer was not found. |
| `409 Conflict` | The offer cannot be rejected because of its current state, for example it has already been accepted. |
