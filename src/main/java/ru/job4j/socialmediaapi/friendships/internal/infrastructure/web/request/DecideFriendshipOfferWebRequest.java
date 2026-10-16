package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Body of {@code POST /api/v1/friendship-offer/accept} and {@code /reject}: the bodies of both
 * endpoints are the same in the contract.
 *
 * @param offerId offer to decide
 * @param actorId user who decides; must be the recipient of the offer.
 *                Taken from the body until authentication (Keycloak) is added, then from the token
 */
public record DecideFriendshipOfferWebRequest(
        @NotNull UUID offerId,
        @NotNull UUID actorId
) {
}
