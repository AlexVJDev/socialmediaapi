package ru.job4j.socialmediaapi.domain.offerfriendship;

public enum OfferFriendshipStatus {
    /** The offer waits for the recipient's decision. */
    PENDING,
    /** The recipient accepted the offer; the users became friends. */
    ACCEPTED,
    /** The recipient rejected the offer. */
    REJECTED
}
