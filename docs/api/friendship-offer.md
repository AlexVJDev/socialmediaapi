# Creating a friendship offer

This method creates a friendship offer from one user to another.

When the offer is created successfully:

- a request is created in the `PENDING` state;
- the initiator automatically subscribes to the recipient;
- friendship between the users is not created yet — it appears only after the offer is accepted.

## HTTP request

```http
POST /api/offer-friendships/create
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
| `fromUserId` | `number` | Yes | Identifier of the user who sends the friendship offer. |
| `toUserId` | `number` | Yes | Identifier of the user who receives the friendship offer. |

## Successful response

If the offer is created, the API returns `201 Created` and the created request data.

```http
HTTP/1.1 201 Created
Content-Type: application/json
```

```json
{
  "id": 10,
  "fromUserId": 1,
  "toUserId": 2,
  "status": "PENDING",
  "createdAt": "2026-09-22T10:15:00Z"
}
```

### Response fields

| Field | Type | Description |
| --- | --- | --- |
| `id` | `number` | Identifier of the created friendship offer. |
| `fromUserId` | `number` | Identifier of the initiating user. |
| `toUserId` | `number` | Identifier of the recipient user. |
| `status` | `string` | Current offer state. For a new request — `PENDING`. |
| `createdAt` | `string` | Offer creation date and time in ISO 8601 format. |

## Processing rules

- A user cannot send a friendship offer to themselves.
- A repeated offer for an existing request in the `PENDING` state does not create a duplicate.
- If the offer has already been accepted, creating it again does not change the friendship state.
- If the offer has been rejected, creating it again ends with a conflict.

## Idempotency

A repeated HTTP request is possible when the client does not receive a response due to a network error.

- Re-creating an offer in the `PENDING` state does not create a second request.
- Re-creating an offer does not create a second subscription from the initiator to the recipient.
- If the offer has already been accepted and is in the `ACCEPTED` state, the repeated call returns the already reached result without changing friendship or subscriptions.
- If the offer has already been rejected and is in the `REJECTED` state, the repeated call ends with a conflict because a rejected offer cannot be automatically moved back to `PENDING`.

## Possible errors

| HTTP status | Case |
| --- | --- |
| `400 Bad Request` | Invalid request body or missing required fields. |
| `404 Not Found` | Initiating user or recipient user was not found. |
| `409 Conflict` | The offer cannot be created because of the current request state, for example after rejection. |
