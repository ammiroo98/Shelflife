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
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
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
                new MemberController(members), new LoanController(loans, members, copies, checkout, returns))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(Jackson2ObjectMapperBuilder.json()
                        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build()))
                .build();
    }

    @Test
    void createsItemWithServerAssignedId() throws Exception {
        when(items.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String body = mvc.perform(post("/api/v1/items").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"The Hobbit\"}"))
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
        mvc.perform(post("/api/v1/copies").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"%s\"}".formatted(item.getId())))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.itemId").value(item.getId().toString()));
    }

    @Test
    void readsCopyWithItemIdOnly() throws Exception {
        when(copies.findById(copy.getId())).thenReturn(Optional.of(copy));
        mvc.perform(get("/api/v1/copies/{id}", copy.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itemId").value(item.getId().toString()))
                .andExpect(jsonPath("$.item").doesNotExist());
    }

    @Test
    void mapsProxyRelationshipToIdWithoutSerializingItsState() throws Exception {
        Item proxy = new ProxyItem(item.getId(), item.getTitle());
        when(copies.findById(copy.getId())).thenReturn(Optional.of(new Copy(copy.getId(), proxy)));

        mvc.perform(get("/api/v1/copies/{id}", copy.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(item.getId().toString()))
                .andExpect(jsonPath("$.item").doesNotExist())
                .andExpect(jsonPath("$.item.hibernateLazyInitializer").doesNotExist())
                .andExpect(jsonPath("$.item.handler").doesNotExist());
    }

    @Test
    void createsActiveMember() throws Exception {
        when(members.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mvc.perform(post("/api/v1/members").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Alex\"}"))
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
        mvc.perform(post("/api/v1/loans").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"%s\",\"copyId\":\"%s\"}".formatted(member.getId(), copy.getId())))
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
                .andExpect(jsonPath("$.copyId").value(copy.getId().toString()))
                .andExpect(jsonPath("$.memberId").value(member.getId().toString()))
                .andExpect(jsonPath("$.copy").doesNotExist())
                .andExpect(jsonPath("$.member").doesNotExist())
                .andExpect(jsonPath("$.borrowedOn").value("2026-10-22"))
                .andExpect(jsonPath("$.dueOn").value("2026-11-05"))
                .andExpect(jsonPath("$.returnedOn").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void delegatesReturnToService() throws Exception {
        loan.returnOn(LocalDate.of(2026, 11, 2));
        when(returns.returnLoan(loan.getId())).thenReturn(loan);
        mvc.perform(post("/api/v1/loans/{id}/return", loan.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.returnedOn").value("2026-11-02"))
                .andExpect(jsonPath("$.copyId").value(copy.getId().toString()))
                .andExpect(jsonPath("$.memberId").value(member.getId().toString()));
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
        mvc.perform(post("/api/v1/loans").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"%s\",\"copyId\":\"%s\"}".formatted(member.getId(), copy.getId()))).andExpect(status().isNotFound());
        verifyNoInteractions(checkout);
    }

    @Test
    void missingCheckoutCopyPreventsCheckout() throws Exception {
        when(members.findById(member.getId())).thenReturn(Optional.of(member));
        mvc.perform(post("/api/v1/loans").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"%s\",\"copyId\":\"%s\"}".formatted(member.getId(), copy.getId()))).andExpect(status().isNotFound());
        verifyNoInteractions(checkout);
    }

    @Test
    void missingCopyItemPreventsCreation() throws Exception {
        mvc.perform(post("/api/v1/copies").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"%s\"}".formatted(item.getId())))
                .andExpect(status().isNotFound());
        verifyNoInteractions(copies);
    }

    @Test
    void missingBodyOrMalformedUuidReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/items").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/copies").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"not-a-uuid\"}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/loans/not-a-uuid")).andExpect(status().isBadRequest());
    }

    // Relationship mapping must read only the identifier, not entity/proxy state.
    public static class ProxyItem extends Item implements HibernateProxy {

        public ProxyItem(UUID id, String title) {
            super(id, title);
        }

        @Override
        public String getTitle() {
            throw new AssertionError("Mapping must not read the related Item title");
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
