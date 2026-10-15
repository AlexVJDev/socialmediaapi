package ru.job4j.socialmediaapi.adapter.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/offer-friendships")
@RequiredArgsConstructor
public class FriendshipOfferController {

    /** Use case that creates friendship offers. */
    private final CreateFriendshipOfferUseCase createFriendshipOffer;

    /** Maps web DTOs to use case requests and results back. */
    private final FriendshipOfferWebMapper mapper;

    /**
     * {@code 201} with the new offer; a repeated offer is answered with {@code 409}
     * by {@code GlobalExceptionHandler}, with the code of the existing offer's status.
     *
     * @param request sender and recipient of the offer
     * @return the created offer
     */
    @PostMapping("/create")
    public ResponseEntity<CreateFriendshipOfferWebResponse> create(
            @Valid @RequestBody CreateFriendshipOfferWebRequest request
    ) {
        var result = createFriendshipOffer.create(mapper.toUseCaseRequest(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(result));
    }
}
