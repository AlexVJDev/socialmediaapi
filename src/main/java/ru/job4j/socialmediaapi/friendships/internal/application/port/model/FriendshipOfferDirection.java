package ru.job4j.socialmediaapi.friendships.internal.application.port.model;

/**
 * Направление заявок относительно пользователя: какой стороной заявки он является.
 */
public enum FriendshipOfferDirection {

    /** Пользователь — получатель заявки ({@code to_user_id}). */
    INCOMING,

    /** Пользователь — отправитель заявки ({@code from_user_id}). */
    OUTGOING
}
