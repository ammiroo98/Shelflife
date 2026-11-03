package com.amiroo.shelflife.catalog;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ResponseEntity<Copy> create(@RequestParam UUID itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + itemId));
        Copy saved = repository.save(new Copy(UUID.randomUUID(), item));
        return ResponseEntity.created(URI.create("/api/v1/copies/" + saved.getId())).body(saved);
    }

    @GetMapping("/{id}")
    public Copy findById(@PathVariable UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Copy not found: " + id));
    }
}
