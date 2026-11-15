package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.Customer;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock private CustomerRepository customerRepository;

    private CustomerServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CustomerServiceImpl(customerRepository);
        lenient().when(customerRepository.save(any(Customer.class))).thenAnswer(call -> call.getArgument(0));
    }

    private static Customer customer(String name, String phone) {
        Customer customer = Customer.builder().name(name).phone(phone).build();
        customer.setId(UUID.randomUUID());
        return customer;
    }

    @Test
    void theSameNumberInAnotherSpellingFindsTheExistingCustomerAndKeepsItsName() {
        Customer existing = customer("Amina Njeri", "0712345678");
        when(customerRepository.findAllByPhoneIn(anyCollection())).thenReturn(List.of(existing));

        Customer result = service.findOrCreate("Someone Else", "+254 712 345 678", "a@b.com", "Nairobi");

        assertThat(result).isSameAs(existing);
        assertThat(result.getName()).isEqualTo("Amina Njeri");
        // Blanks are filled in.
        assertThat(result.getEmail()).isEqualTo("a@b.com");
        assertThat(result.getCity_town()).isEqualTo("Nairobi");
    }

    @Test
    void existingEmailAndCityAreNotOverwritten() {
        Customer existing = customer("Amina", "0712345678");
        existing.setEmail("old@b.com");
        existing.setCity_town("Mombasa");
        when(customerRepository.findAllByPhoneIn(anyCollection())).thenReturn(List.of(existing));

        service.findOrCreate("Amina", "0712345678", "new@b.com", "Nairobi");

        assertThat(existing.getEmail()).isEqualTo("old@b.com");
        assertThat(existing.getCity_town()).isEqualTo("Mombasa");
    }

    @Test
    void newCustomersAreStoredWithTheCanonicalPhone() {
        when(customerRepository.findAllByPhoneIn(anyCollection())).thenReturn(List.of());

        Customer created = service.findOrCreate("Amina", "+254712345678", null, null);

        assertThat(created.getPhone()).isEqualTo("0712345678");
    }

    @Test
    void anOldSpellingIsMovedToTheCanonicalFormWhenThatIsFree() {
        Customer legacy = customer("Amina", "254712345678");
        when(customerRepository.findAllByPhoneIn(anyCollection())).thenReturn(List.of(legacy));
        when(customerRepository.findAllByPhone("0712345678")).thenReturn(List.of());

        service.findOrCreate("Amina", "0712345678", null, null);

        assertThat(legacy.getPhone()).isEqualTo("0712345678");
    }

    @Test
    void noPhoneAlwaysMeansANewCustomerNeverASharedOne() {
        Customer first = service.findOrCreate("Walk-in", null, null, null);
        Customer second = service.findOrCreate("Walk-in", "UNKNOWN", null, null);

        assertThat(first.getPhone()).isNull();
        assertThat(second.getPhone()).isNull();
        verify(customerRepository, never()).findAllByPhoneIn(anyCollection());
    }

    @Test
    void aDeletedCustomerWithTheSamePhoneIsRestoredInsteadOfHittingTheUniqueIndex() {
        Customer deleted = customer("Old Name", "0712345678");
        deleted.setDeletedAt(LocalDateTime.now().minusDays(3));
        when(customerRepository.findAllByPhoneIn(anyCollection())).thenReturn(List.of(deleted));

        Customer result = service.findOrCreate("Amina", "0712345678", null, null);

        assertThat(result).isSameAs(deleted);
        assertThat(result.getDeletedAt()).isNull();
        assertThat(result.getName()).isEqualTo("Amina");
    }

    @Test
    void aLiveCustomerIsPreferredOverADeletedOne() {
        Customer deleted = customer("Old", "0712345678");
        deleted.setDeletedAt(LocalDateTime.now());
        Customer live = customer("Live", "254712345678");
        when(customerRepository.findAllByPhoneIn(anyCollection())).thenReturn(List.of(deleted, live));

        assertThat(service.findOrCreate("X", "0712345678", null, null)).isSameAs(live);
    }

    @Test
    void deleteIsSoftSoOrdersKeepTheirCustomer() {
        Customer customer = customer("Amina", "0712345678");
        when(customerRepository.findByIdAndDeletedAtIsNull(customer.getId())).thenReturn(Optional.of(customer));

        service.deleteCustomerById(customer.getId());

        assertThat(customer.getDeletedAt()).isNotNull();
        verify(customerRepository, never()).deleteById(any());
    }

    @Test
    void deletingAnUnknownCustomerIsANotFound() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteCustomerById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
