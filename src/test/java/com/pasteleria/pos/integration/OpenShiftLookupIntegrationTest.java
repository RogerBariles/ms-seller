package com.pasteleria.pos.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pasteleria.pos.domain.entity.CashRegister;
import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.domain.entity.Shift;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.CashRegisterStatus;
import com.pasteleria.pos.domain.enums.ShiftStatus;
import com.pasteleria.pos.domain.enums.UserRole;
import com.pasteleria.pos.repository.ShiftRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

/** Con varios turnos abiertos en la empresa, la búsqueda del "primero" no debe fallar. */
@DataJpaTest
@ActiveProfiles("test")
class OpenShiftLookupIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ShiftRepository shiftRepository;

    private Company company;
    private CashRegister cashRegister;
    private Shift first;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(UUID.randomUUID());
        company.setName("Centro");
        company.setActive(true);
        em.persist(company);

        User a = user("a");
        User b = user("b");

        cashRegister = new CashRegister();
        cashRegister.setId(UUID.randomUUID());
        cashRegister.setBusinessDate(LocalDate.now());
        cashRegister.setInitialCash(BigDecimal.ZERO);
        cashRegister.setStatus(CashRegisterStatus.OPEN);
        cashRegister.setOpenedBy(a);
        cashRegister.setCompany(company);
        cashRegister.setOpenedAt(OffsetDateTime.now());
        em.persist(cashRegister);

        first = shift(a, OffsetDateTime.now().minusHours(2));
        shift(b, OffsetDateTime.now().minusHours(1));
        em.flush();
    }

    @Test
    void firstOpenShiftWithSeveralOpenShifts() {
        Optional<Shift> found = shiftRepository.findFirstByStatusOrderByStartedAtAsc(ShiftStatus.OPEN, company.getId());
        assertTrue(found.isPresent());
        assertEquals(first.getId(), found.get().getId());
    }

    private User user(String username) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName(username);
        user.setUsername(username);
        user.setPasswordHash("pwd");
        user.setRole(UserRole.SELLER);
        user.setActive(true);
        user.setCompany(company);
        em.persist(user);
        return user;
    }

    private Shift shift(User seller, OffsetDateTime startedAt) {
        Shift shift = new Shift();
        shift.setId(UUID.randomUUID());
        shift.setCashRegister(cashRegister);
        shift.setSeller(seller);
        shift.setInitialCash(BigDecimal.ZERO);
        shift.setStatus(ShiftStatus.OPEN);
        shift.setCompany(company);
        shift.setStartedAt(startedAt);
        em.persist(shift);
        return shift;
    }
}
