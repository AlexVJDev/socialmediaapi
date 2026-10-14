package ru.job4j.socialmediaapi.adapter.in.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.job4j.socialmediaapi.adapter.in.web.dto.CreateFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.adapter.in.web.dto.CreateFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.adapter.in.web.mapper.FriendshipOfferWebMapper;
import ru.job4j.socialmediaapi.application.port.in.CreateFriendshipOfferUseCase;
import ru.job4j.socialmediaapi.application.port.in.CreateFriendshipOfferUseCase.CreateFriendshipOfferResult.Created;

@RestController
@RequestMapping("/api/offer-friendships")
public class FriendshipOfferController {

    /** Use case that creates friendship offers. */
    private final CreateFriendshipOfferUseCase createFriendshipOffer;

    /** Maps web DTOs to use case commands and results back. */
    private final FriendshipOfferWebMapper mapper;

    public FriendshipOfferController(
            CreateFriendshipOfferUseCase createFriendshipOffer,
            FriendshipOfferWebMapper mapper
    ) {
        this.createFriendshipOffer = createFriendshipOffer;
        this.mapper = mapper;
    }

    /**
     * {@code 201} with the new offer, or {@code 204} without a body
     * if the offer already waits for a decision.
     *
     * @param request sender and recipient of the offer
     * @return the created offer, or an empty response for a repeated offer
     */
    @PostMapping("/create")
    public ResponseEntity<CreateFriendshipOfferWebResponse> create(
            @Valid @RequestBody CreateFriendshipOfferWebRequest request
    ) {
        var result = createFriendshipOffer.create(mapper.toCommand(request));
        if (result instanceof Created created) {
            return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
        }
        return ResponseEntity.noContent().build();
    }
}
