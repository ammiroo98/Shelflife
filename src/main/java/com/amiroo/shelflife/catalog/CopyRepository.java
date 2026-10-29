package com.amiroo.shelflife.catalog;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CopyRepository extends JpaRepository<Copy, UUID> {
}
