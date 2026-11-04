package com.amiroo.shelflife.catalog;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import com.amiroo.shelflife.catalog.dto.CreateCopyRequest;
import com.amiroo.shelflife.catalog.dto.CopyResponse;
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
@RequestMapping("/api/v1/copies")
public class CopyController {

    private final CopyRepository repository;
    private final ItemRepository itemRepository;

    public CopyController(CopyRepository repository, ItemRepository itemRepository) {
        this.repository = repository;
        this.itemRepository = itemRepository;
    }

    @PostMapping
    public ResponseEntity<CopyResponse> create(@Valid @RequestBody CreateCopyRequest request) {
        UUID itemId = request.itemId();
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + itemId));
        Copy saved = repository.save(new Copy(UUID.randomUUID(), item));
        return ResponseEntity.created(URI.create("/api/v1/copies/" + saved.getId())).body(CopyResponse.from(saved));
    }

    @GetMapping
    public List<CopyResponse> findAll() {
        return repository.findAll().stream().map(CopyResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CopyResponse findById(@PathVariable UUID id) {
        Copy copy = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Copy not found: " + id));
        return CopyResponse.from(copy);
    }
}
