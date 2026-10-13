package ru.job4j.socialmediaapi.adapter.in.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.job4j.socialmediaapi.adapter.in.web.dto.CreateOfferFriendshipWebRequest;
import ru.job4j.socialmediaapi.adapter.in.web.dto.CreateOfferFriendshipWebResponse;
import ru.job4j.socialmediaapi.adapter.in.web.mapper.OfferFriendshipWebMapper;
import ru.job4j.socialmediaapi.application.port.in.CreateOfferFriendshipUseCase;
import ru.job4j.socialmediaapi.application.port.in.CreateOfferFriendshipUseCase.CreateOfferFriendshipResult.Created;

@RestController
@RequestMapping("/api/offer-friendships")
public class OfferFriendshipController {

    private final CreateOfferFriendshipUseCase createOfferFriendship;

    private final OfferFriendshipWebMapper mapper;

    public OfferFriendshipController(
            CreateOfferFriendshipUseCase createOfferFriendship,
            OfferFriendshipWebMapper mapper
    ) {
        this.createOfferFriendship = createOfferFriendship;
        this.mapper = mapper;
    }

    /**
     * {@code 201} with the new offer, or {@code 204} without a body
     * if the offer already waits for a decision.
     */
    @PostMapping("/create")
    public ResponseEntity<CreateOfferFriendshipWebResponse> create(
            @Valid @RequestBody CreateOfferFriendshipWebRequest request
    ) {
        var result = createOfferFriendship.create(mapper.toCommand(request));
        if (result instanceof Created created) {
            return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
        }
        return ResponseEntity.noContent().build();
    }
}
