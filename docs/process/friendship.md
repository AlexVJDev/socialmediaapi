# Friendship flow

This document describes how a friend request works when **Alice** sends a request to **Boris**, and how this request affects following and friendship relations.

---

## Quick overview

```text
Alice sends Boris a friend request
    ↓
The system creates a PENDING request
    ↓
Alice starts following Boris
    ↓
Boris accepts the request
    ↓
The request status changes to ACCEPTED
    ↓
The system creates a friendship between Alice and Boris
    ↓
Boris starts following Alice
```

## Main participants

| Participant | Role in the process |
| --- | --- |
| **Alice** | Sends a friend request and starts following Boris. |
| **Boris** | Receives the request and decides whether to accept it. |
| **System** | Stores request status, creates follow relations and creates friendship. |

## Action summary

| Action | Initiator | Precondition | Result |
| --- | --- | --- | --- |
| Open Boris's profile | Alice | Alice and Boris exist in the system. | Alice can view Boris's profile and start the friend request flow. |
| Send a friend request | Alice | Alice and Boris are different users; there is no active request from Alice to Boris; Alice and Boris are not already friends. | The system creates a request with the `PENDING` status and Alice starts following Boris. |
| Notify about the incoming request | System | A `PENDING` friend request from Alice to Boris exists. | Boris receives information about the incoming friend request. |
| Accept the friend request | Boris | A `PENDING` friend request from Alice to Boris exists. | The request changes to `ACCEPTED`; mutual friendship is created; Boris starts following Alice. |
| Reject the friend request | Boris | A `PENDING` friend request from Alice to Boris exists. | The request changes to `REJECTED`; friendship is not created; Alice remains a follower of Boris. |
| Keep the request pending | Boris | Boris neither accepts nor rejects the request. | The request remains `PENDING`; Alice continues following Boris until another rule changes this relation. |

## Process description

### 1. Alice opens Boris's profile

While using the application, **Alice** opens **Boris's** profile — a user whose updates she wants to follow and with whom she may want to communicate later.

On Boris's profile, Alice clicks **Add Friend**.

### 2. The system creates a friend request

After Alice sends the request, the system:

- creates a friend request with the `PENDING` status;
- automatically adds Boris to the list of users Alice follows;
- makes Alice a `follower` of Boris.

At this point, Alice and Boris are **not friends yet**. Alice has only sent a friend request and started following Boris.

### 3. Alice follows Boris

Following Boris allows Alice to quickly find his profile in a separate **Following** section and see updates available to her, for example:

- new posts;
- photos;
- videos;
- audio recordings;
- other events or content available to followers.

### 4. Boris receives the request

Boris receives a notification about the incoming friend request from Alice.

Depending on the application's implementation, this notification may be delivered as:

- an in-app notification;
- a push notification;
- an email.

### 5. Boris accepts the request

Boris opens the request and clicks **Accept**.

After Boris accepts the request, the system:

- changes the request status from `PENDING` to `ACCEPTED`;
- creates a mutual friendship relationship between Alice and Boris;
- adds Boris to Alice's friends list;
- adds Alice to Boris's friends list;
- automatically makes Boris a follower of Alice if such a follow relationship does not already exist.

As a result, Alice and Boris become friends and also follow each other.

## Alternative scenario: Boris rejects the request

Boris may decide not to accept Alice's friend request.

In this case, Boris opens the incoming request and clicks **Reject**.

After Boris rejects the request, the system:

- changes the request status from `PENDING` to `REJECTED`;
- does not create a friendship relationship between Alice and Boris;
- does not add Boris to Alice's friends list;
- does not add Alice to Boris's friends list;
- does not create a reverse follow relationship from Boris to Alice;
- keeps Alice as a follower of Boris unless another application rule or privacy setting prevents it.

As a result, Alice and Boris are not friends, but Alice may continue following Boris.

## Request states

| State | Meaning |
| --- | --- |
| `PENDING` | The request has been sent, but Boris has not accepted it yet. Alice follows Boris. |
| `ACCEPTED` | Boris accepted the request. Alice and Boris are friends and follow each other. |
| `REJECTED` | Boris rejected the request. Friendship is not created, but Alice remains a follower of Boris. |

## Friendship benefits

Friendship may provide additional functionality compared with a regular follow relationship, for example:

- sending private text messages;
- sending audio and video messages;
- making audio and video calls;
- viewing the `online` status;
- viewing the user's last seen time;
- accessing posts or other information available only to friends.

The exact set of available features depends on the user's privacy settings and the application's rules.

## If the request remains pending

If Boris neither accepts nor rejects the request, it remains in the `PENDING` state.

Alice continues to follow Boris until one of the following happens:

- Alice unfollows Boris herself;
- Boris's privacy settings restrict this follow relationship;
- the application rules cancel or limit pending requests.
