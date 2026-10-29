package com.amiroo.shelflife.lending;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

    long countByMember_IdAndReturnedOnIsNull(UUID memberId);

    boolean existsByCopy_IdAndReturnedOnIsNull(UUID copyId);
}
