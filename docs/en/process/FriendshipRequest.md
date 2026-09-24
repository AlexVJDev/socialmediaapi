# `OfferFriendship` Transition Table

This document describes friendship request transitions for the `offerFriendship`, `acceptFriendship`, and `rejectFriendship` commands.

## State Transitions

| Command | Initial State | New State | Subscription Change | Friendship Change | Repeated Call Result |
| --- | --- | --- | --- | --- | --- |
| `offerFriendship` | Request does not exist | `PENDING` | The initiator subscribes to the recipient | No changes | `204 No Content`, state does not change |
| `acceptFriendship` | `PENDING` | `ACCEPTED` | The recipient subscribes to the initiator if the reverse subscription does not already exist | A bidirectional friendship is created between the initiator and the recipient | `204 No Content`, state does not change |
| `rejectFriendship` | `PENDING` | `REJECTED` | No changes: the initiator remains subscribed to the recipient | No changes: friendship is not created | `204 No Content`, state does not change |
| `offerFriendship` | `PENDING` | `PENDING` | No changes: the initiator is already subscribed to the recipient | No changes | `204 No Content`, state does not change |
| `offerFriendship` | `ACCEPTED` | `ACCEPTED` | No changes: the users already have the required subscriptions | No changes: friendship already exists | `204 No Content`, state does not change |
| `offerFriendship` | `REJECTED` | `REJECTED` | No changes: a rejected offer cannot be moved back to `PENDING` | No changes | `409 Conflict`, `OfferFriendshipAlreadyRejectedException`, state does not change |
| `acceptFriendship` | `ACCEPTED` | `ACCEPTED` | No changes: the reverse subscription already exists or should already have been created | No changes: friendship already exists | `204 No Content`, state does not change |
| `rejectFriendship` | `ACCEPTED` | `ACCEPTED` | No changes | No changes: an already created friendship is not canceled by the reject command | `409 Conflict`, `RejectFriendshipAlreadyAcceptedException`, state does not change |
| `acceptFriendship` | `REJECTED` | `REJECTED` | No changes | No changes: a rejected request does not create a friendship when accepted again | `409 Conflict`, `AcceptedFriendshipAlreadyreRejectedException`, state does not change |
| `rejectFriendship` | `REJECTED` | `REJECTED` | No changes | No changes: friendship was not created | `204 No Content`, state does not change |

## Idempotency Rules

- A repeated command call must not create duplicate requests, subscriptions, or friendship links.
- If the target state has already been reached, the system returns `204 No Content` and does not change any data.
- A repeated `offerFriendship` for a request in the `PENDING` state returns `204 No Content` because the request is already waiting for a decision.
- A repeated `offerFriendship` for a request in the `ACCEPTED` state returns `204 No Content` because the friendship already exists.
- A repeated `offerFriendship` for a request in the `REJECTED` state returns `409 Conflict` through `OfferFriendshipAlreadyRejectedException` because a rejected offer cannot be moved back to `PENDING`.
- Rejecting a request does not cancel the initiator's subscription to the recipient.
- Accepting a request creates the friendship and the reverse subscription atomically.

## Error Cases

Each error case uses a separate exception type. This explicitly communicates the reason for the failure and allows the outer layer to select the appropriate HTTP response.

| Case | Command | Initial State | New State | Subscription Change | Friendship Change | Exception |
| --- | --- | --- | --- | --- | --- | --- |
| A user offers friendship to themselves | `offerFriendship` | Request does not exist | No changes | No changes | No changes | `OfferFriendshipToSelfException` |
| Offer to accept was not found | `acceptFriendship` | Request does not exist | No changes | No changes | No changes | `FriendshipRequestNotFoundException` |
| Offer to reject was not found | `rejectFriendship` | Request does not exist | No changes | No changes | No changes | `FriendshipRequestNotFoundException` |
| Repeated offer after rejection | `offerFriendship` | `REJECTED` | `REJECTED` | No changes | No changes | `OfferFriendshipAlreadyRejectedException` |

## Repeated Friendship Offer After Rejection

After the `REJECTED` state, a repeated `offerFriendship` call does not create a new offer and ends with a conflict.

If the product needs to allow a repeated offer after some time, a separate business rule will be required. Such a rule must explicitly describe when a rejected offer can be replaced with a new one, for example after a waiting period expires.
