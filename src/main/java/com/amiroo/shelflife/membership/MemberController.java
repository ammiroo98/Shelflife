package com.amiroo.shelflife.membership;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import com.amiroo.shelflife.membership.dto.CreateMemberRequest;
import com.amiroo.shelflife.membership.dto.MemberResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberRepository repository;

    public MemberController(MemberRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<MemberResponse> create(@Valid @RequestBody CreateMemberRequest request) {
        Member saved = repository.save(new Member(UUID.randomUUID(), request.name()));
        return ResponseEntity.created(URI.create("/api/v1/members/" + saved.getId())).body(MemberResponse.from(saved));
    }

    @GetMapping("/{id}")
    public MemberResponse findById(@PathVariable UUID id) {
        Member member = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found: " + id));
        return MemberResponse.from(member);
    }
}
