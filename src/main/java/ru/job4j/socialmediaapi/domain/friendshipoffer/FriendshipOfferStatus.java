package ru.job4j.socialmediaapi.domain.friendshipoffer;

public enum FriendshipOfferStatus {
    /** The offer waits for the recipient's decision. */
    PENDING,
    /** The recipient accepted the offer; the users became friends. */
    ACCEPTED,
    /** The recipient rejected the offer. */
    REJECTED
}
