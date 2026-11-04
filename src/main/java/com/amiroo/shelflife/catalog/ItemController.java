package com.amiroo.shelflife.catalog;

import java.net.URI;
import java.util.UUID;

import com.amiroo.shelflife.catalog.dto.CreateItemRequest;
import com.amiroo.shelflife.catalog.dto.ItemResponse;
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
@RequestMapping("/api/v1/items")
public class ItemController {

    private final ItemRepository repository;

    public ItemController(ItemRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<ItemResponse> create(@RequestBody CreateItemRequest request) {
        Item saved = repository.save(new Item(UUID.randomUUID(), request.title()));
        return ResponseEntity.created(URI.create("/api/v1/items/" + saved.getId())).body(ItemResponse.from(saved));
    }

    @GetMapping("/{id}")
    public ItemResponse findById(@PathVariable UUID id) {
        Item item = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + id));
        return ItemResponse.from(item);
    }
}
