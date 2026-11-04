package com.amiroo.shelflife.lending;

import java.net.URI;
import java.util.UUID;

import com.amiroo.shelflife.lending.dto.CheckoutRequest;
import com.amiroo.shelflife.lending.dto.LoanResponse;
import com.amiroo.shelflife.catalog.Copy;
import com.amiroo.shelflife.catalog.CopyRepository;
import com.amiroo.shelflife.membership.Member;
import com.amiroo.shelflife.membership.MemberRepository;
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
@RequestMapping("/api/v1/loans")
public class LoanController {

    private final LoanRepository repository;
    private final MemberRepository memberRepository;
    private final CopyRepository copyRepository;
    private final CheckoutService checkoutService;
    private final ReturnService returnService;

    public LoanController(LoanRepository repository, MemberRepository memberRepository,
            CopyRepository copyRepository, CheckoutService checkoutService, ReturnService returnService) {
        this.repository = repository;
        this.memberRepository = memberRepository;
        this.copyRepository = copyRepository;
        this.checkoutService = checkoutService;
        this.returnService = returnService;
    }

    @PostMapping
    public ResponseEntity<LoanResponse> checkout(@RequestBody CheckoutRequest request) {
        UUID memberId = request.memberId();
        UUID copyId = request.copyId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found: " + memberId));
        Copy copy = copyRepository.findById(copyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Copy not found: " + copyId));
        Loan loan = checkoutService.checkout(member, copy);
        return ResponseEntity.created(URI.create("/api/v1/loans/" + loan.getId())).body(LoanResponse.from(loan));
    }

    @GetMapping("/{id}")
    public LoanResponse findById(@PathVariable UUID id) {
        Loan loan = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found: " + id));
        return LoanResponse.from(loan);
    }

    @PostMapping("/{id}/return")
    public LoanResponse returnLoan(@PathVariable UUID id) {
        return LoanResponse.from(returnService.returnLoan(id));
    }
}
