package com.amiroo.shelflife.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import com.amiroo.shelflife.catalog.Copy;
import com.amiroo.shelflife.catalog.CopyController;
import com.amiroo.shelflife.catalog.CopyRepository;
import com.amiroo.shelflife.catalog.Item;
import com.amiroo.shelflife.catalog.ItemController;
import com.amiroo.shelflife.catalog.ItemRepository;
import com.amiroo.shelflife.lending.CheckoutService;
import com.amiroo.shelflife.lending.BorrowingException;
import com.amiroo.shelflife.lending.LoanNotFoundException;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
                .setControllerAdvice(new ApiExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(Jackson2ObjectMapperBuilder.json()
                        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build()))
                .build();
    }

    @Test
    void listsItemsAsResponseDtos() throws Exception {
        Item second = new Item(UUID.randomUUID(), "Dune");
        when(items.findAll()).thenReturn(List.of(item, second));

        mvc.perform(get("/api/v1/items"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        [{"id":"%s","title":"The Hobbit"},{"id":"%s","title":"Dune"}]
                        """.formatted(item.getId(), second.getId()), true));
        verify(items).findAll();
    }

    @Test
    void listsCopiesWithItemIdsOnly() throws Exception {
        Copy second = new Copy(UUID.randomUUID(), new ProxyItem(item.getId(), item.getTitle()));
        when(copies.findAll()).thenReturn(List.of(copy, second));

        mvc.perform(get("/api/v1/copies"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        [{"id":"%s","itemId":"%s"},{"id":"%s","itemId":"%s"}]
                        """.formatted(copy.getId(), item.getId(), second.getId(), item.getId()), true));
        verify(copies).findAll();
    }

    @Test
    void listsMembersAsResponseDtos() throws Exception {
        Member second = new Member(UUID.randomUUID(), "Sam");
        when(members.findAll()).thenReturn(List.of(member, second));

        mvc.perform(get("/api/v1/members"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        [{"id":"%s","name":"Alex","status":"ACTIVE"},
                         {"id":"%s","name":"Sam","status":"ACTIVE"}]
                        """.formatted(member.getId(), second.getId()), true));
        verify(members).findAll();
    }

    @ParameterizedTest
    @ValueSource(strings = {"items", "copies", "members"})
    void emptyListsReturnJsonArrays(String resource) throws Exception {
        when(items.findAll()).thenReturn(List.of());
        when(copies.findAll()).thenReturn(List.of());
        when(members.findAll()).thenReturn(List.of());

        mvc.perform(get("/api/v1/" + resource))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string("[]"));
        switch (resource) {
            case "items" -> verify(items).findAll();
            case "copies" -> verify(copies).findAll();
            case "members" -> verify(members).findAll();
            default -> throw new AssertionError(resource);
        }
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
        for (String resource : new String[] {"items", "copies", "members", "loans"}) {
            mvc.perform(get("/api/v1/" + resource + "/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.trace").doesNotExist());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Suspended members cannot borrow.",
            "Member has reached the active loan limit of 2.", "Copy already has an active loan."})
    void checkoutBusinessFailuresReturnConflict(String message) throws Exception {
        when(members.findById(member.getId())).thenReturn(Optional.of(member));
        when(copies.findById(copy.getId())).thenReturn(Optional.of(copy));
        when(checkout.checkout(member, copy)).thenThrow(new BorrowingException(message));

        mvc.perform(post("/api/v1/loans").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"%s\",\"copyId\":\"%s\"}".formatted(member.getId(), copy.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void missingReturnLoanReturnsNotFound() throws Exception {
        when(returns.returnLoan(loan.getId())).thenThrow(new LoanNotFoundException(loan.getId()));

        mvc.perform(post("/api/v1/loans/{id}/return", loan.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Loan not found: " + loan.getId()));
    }

    @Test
    void alreadyReturnedLoanReturnsConflict() throws Exception {
        when(returns.returnLoan(loan.getId())).thenThrow(new BorrowingException("Loan has already been returned."));

        mvc.perform(post("/api/v1/loans/{id}/return", loan.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Loan has already been returned."));
    }

    @Test
    void unexpectedProgrammingErrorsAreNotConvertedIntoClientErrors() {
        when(items.findById(item.getId())).thenThrow(new IllegalStateException("Unexpected failure"));

        assertThatThrownBy(() -> mvc.perform(get("/api/v1/items/{id}", item.getId())))
                .hasRootCauseInstanceOf(IllegalStateException.class);
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

    @Test
    void malformedJsonReturnsConciseBadRequest() throws Exception {
        mvc.perform(post("/api/v1/items").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Malformed or missing JSON request body."))
                .andExpect(jsonPath("$.trace").doesNotExist());
        verifyNoInteractions(items);
    }

    @Test
    void invalidPathUuidReturnsConciseBadRequest() throws Exception {
        mvc.perform(get("/api/v1/loans/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'."));
        verifyNoInteractions(loans);
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void invalidFieldsReturnBadRequestBeforeAnyRepositoryOrServiceAccess(String path, String body) throws Exception {
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed."))
                .andExpect(jsonPath("$.fieldErrors[0].field").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors[0].message").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(MethodArgumentNotValidException.class));
        verifyNoInteractions(items, copies, members, loans, checkout, returns);
    }

    static Stream<Arguments> invalidRequests() {
        String id = "00000000-0000-0000-0000-000000000001";
        return Stream.of(
                Arguments.of("/api/v1/items", "{}"),
                Arguments.of("/api/v1/items", "{\"title\":null}"),
                Arguments.of("/api/v1/items", "{\"title\":\"\"}"),
                Arguments.of("/api/v1/items", "{\"title\":\"   \"}"),
                Arguments.of("/api/v1/items", "{\"title\":\"\\t\\n\"}"),
                Arguments.of("/api/v1/members", "{}"),
                Arguments.of("/api/v1/members", "{\"name\":null}"),
                Arguments.of("/api/v1/members", "{\"name\":\"\"}"),
                Arguments.of("/api/v1/members", "{\"name\":\"   \"}"),
                Arguments.of("/api/v1/members", "{\"name\":\"\\t\\n\"}"),
                Arguments.of("/api/v1/copies", "{}"),
                Arguments.of("/api/v1/copies", "{\"itemId\":null}"),
                Arguments.of("/api/v1/loans", "{}"),
                Arguments.of("/api/v1/loans", "{\"copyId\":\"%s\"}".formatted(id)),
                Arguments.of("/api/v1/loans", "{\"memberId\":\"%s\"}".formatted(id)),
                Arguments.of("/api/v1/loans", "{\"memberId\":null,\"copyId\":\"%s\"}".formatted(id)),
                Arguments.of("/api/v1/loans", "{\"memberId\":\"%s\",\"copyId\":null}".formatted(id)));
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
