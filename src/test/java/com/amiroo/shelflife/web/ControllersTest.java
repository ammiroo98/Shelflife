package com.amiroo.shelflife.web;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.amiroo.shelflife.catalog.Copy;
import com.amiroo.shelflife.catalog.CopyController;
import com.amiroo.shelflife.catalog.CopyRepository;
import com.amiroo.shelflife.catalog.Item;
import com.amiroo.shelflife.catalog.ItemController;
import com.amiroo.shelflife.catalog.ItemRepository;
import com.amiroo.shelflife.lending.CheckoutService;
import com.amiroo.shelflife.lending.Loan;
import com.amiroo.shelflife.lending.LoanController;
import com.amiroo.shelflife.lending.LoanRepository;
import com.amiroo.shelflife.lending.ReturnService;
import com.amiroo.shelflife.membership.Member;
import com.amiroo.shelflife.membership.MemberController;
import com.amiroo.shelflife.membership.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ControllersTest {

    private final ItemRepository items = mock(ItemRepository.class);
    private final CopyRepository copies = mock(CopyRepository.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final LoanRepository loans = mock(LoanRepository.class);
    private final CheckoutService checkout = mock(CheckoutService.class);
    private final ReturnService returns = mock(ReturnService.class);
    private final Item item = new Item(UUID.randomUUID(), "The Hobbit");
    private final Copy copy = new Copy(UUID.randomUUID(), item);
    private final Member member = new Member(UUID.randomUUID(), "Alex");
    private final Loan loan = new Loan(UUID.randomUUID(), copy, member,
            LocalDate.of(2026, 10, 22), LocalDate.of(2026, 11, 5), null);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ItemController(items), new CopyController(copies, items),
                new MemberController(members), new LoanController(loans, members, copies, checkout, returns)).build();
    }

    @Test
    void createsItemWithServerAssignedId() throws Exception {
        when(items.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String body = mvc.perform(post("/api/v1/items").param("title", "The Hobbit"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("The Hobbit"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("hibernateLazyInitializer");
    }

    @Test
    void readsItem() throws Exception {
        when(items.findById(item.getId())).thenReturn(Optional.of(item));
        mvc.perform(get("/api/v1/items/{id}", item.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("The Hobbit"));
    }

    @Test
    void createsCopyForExistingItem() throws Exception {
        when(items.findById(item.getId())).thenReturn(Optional.of(item));
        when(copies.save(any(Copy.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mvc.perform(post("/api/v1/copies").param("itemId", item.getId().toString()))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.item.id").value(item.getId().toString()));
    }

    @Test
    void readsCopyAndItem() throws Exception {
        when(copies.findById(copy.getId())).thenReturn(Optional.of(copy));
        mvc.perform(get("/api/v1/copies/{id}", copy.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.item.title").value("The Hobbit"));
    }

    @Test
    void excludesHibernateProxyMetadataFromNestedJson() throws Exception {
        Item proxy = new ProxyItem(item.getId(), item.getTitle());
        when(copies.findById(copy.getId())).thenReturn(Optional.of(new Copy(copy.getId(), proxy)));

        mvc.perform(get("/api/v1/copies/{id}", copy.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.item.title").value("The Hobbit"))
                .andExpect(jsonPath("$.item.hibernateLazyInitializer").doesNotExist())
                .andExpect(jsonPath("$.item.handler").doesNotExist());
    }

    @Test
    void createsActiveMember() throws Exception {
        when(members.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mvc.perform(post("/api/v1/members").param("name", "Alex"))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Alex"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void readsMember() throws Exception {
        when(members.findById(member.getId())).thenReturn(Optional.of(member));
        mvc.perform(get("/api/v1/members/{id}", member.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Alex"));
    }

    @Test
    void delegatesCheckoutWithLoadedMemberAndCopy() throws Exception {
        when(members.findById(member.getId())).thenReturn(Optional.of(member));
        when(copies.findById(copy.getId())).thenReturn(Optional.of(copy));
        when(checkout.checkout(member, copy)).thenReturn(loan);
        mvc.perform(post("/api/v1/loans").param("memberId", member.getId().toString())
                        .param("copyId", copy.getId().toString()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/loans/" + loan.getId()))
                .andExpect(jsonPath("$.id").value(loan.getId().toString()));
        verify(checkout).checkout(member, copy);
        verifyNoInteractions(loans);
    }

    @Test
    void readsLoanWithDatesAndRelationships() throws Exception {
        when(loans.findById(loan.getId())).thenReturn(Optional.of(loan));
        mvc.perform(get("/api/v1/loans/{id}", loan.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.copy.item.title").value("The Hobbit"))
                .andExpect(jsonPath("$.member.name").value("Alex"))
                .andExpect(jsonPath("$.borrowedOn").isNotEmpty())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void delegatesReturnToService() throws Exception {
        loan.returnOn(LocalDate.of(2026, 11, 2));
        when(returns.returnLoan(loan.getId())).thenReturn(loan);
        mvc.perform(post("/api/v1/loans/{id}/return", loan.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.returnedOn").isNotEmpty());
        verify(returns).returnLoan(loan.getId());
        verifyNoInteractions(loans);
    }

    @Test
    void missingReadResourceReturnsNotFound() throws Exception {
        mvc.perform(get("/api/v1/items/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/copies/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/members/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/loans/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void missingCheckoutMemberPreventsCheckout() throws Exception {
        mvc.perform(post("/api/v1/loans").param("memberId", member.getId().toString())
                        .param("copyId", copy.getId().toString())).andExpect(status().isNotFound());
        verifyNoInteractions(checkout);
    }

    @Test
    void missingCheckoutCopyPreventsCheckout() throws Exception {
        when(members.findById(member.getId())).thenReturn(Optional.of(member));
        mvc.perform(post("/api/v1/loans").param("memberId", member.getId().toString())
                        .param("copyId", copy.getId().toString())).andExpect(status().isNotFound());
        verifyNoInteractions(checkout);
    }

    @Test
    void missingCopyItemPreventsCreation() throws Exception {
        mvc.perform(post("/api/v1/copies").param("itemId", item.getId().toString()))
                .andExpect(status().isNotFound());
        verifyNoInteractions(copies);
    }

    @Test
    void missingRequiredParameterOrMalformedUuidReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/items")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/loans/not-a-uuid")).andExpect(status().isBadRequest());
    }

    // A small proxy-shaped fixture avoids exposing Mockito's own generated bean properties.
    public static class ProxyItem extends Item implements HibernateProxy {

        public ProxyItem(UUID id, String title) {
            super(id, title);
        }

        @Override
        public LazyInitializer getHibernateLazyInitializer() {
            throw new AssertionError("Hibernate proxy metadata must not be serialized");
        }

        @Override
        public Object writeReplace() {
            return this;
        }
    }
}
