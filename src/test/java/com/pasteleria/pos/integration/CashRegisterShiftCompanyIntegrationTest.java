package com.pasteleria.pos.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pasteleria.pos.domain.entity.CashRegister;
import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.domain.entity.Shift;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.CashRegisterStatus;
import com.pasteleria.pos.domain.enums.ShiftStatus;
import com.pasteleria.pos.domain.enums.UserRole;
import com.pasteleria.pos.repository.CashRegisterRepository;
import com.pasteleria.pos.repository.ShiftRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class CashRegisterShiftCompanyIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CashRegisterRepository cashRegisterRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    private Company persistCompany(String name) {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setName(name);
        company.setActive(true);
        return em.persistAndFlush(company);
    }

    private User persistUser(String username, Company company) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName(username);
        user.setUsername(username);
        user.setPasswordHash("pwd");
        user.setRole(UserRole.SELLER);
        user.setActive(true);
        user.setCompany(company);
        return em.persistAndFlush(user);
    }

    private CashRegister persistCashRegister(LocalDate date, CashRegisterStatus status, User openedBy, Company company) {
        CashRegister cr = new CashRegister();
        cr.setId(UUID.randomUUID());
        cr.setBusinessDate(date);
        cr.setInitialCash(new BigDecimal("1000.00"));
        cr.setStatus(status);
        cr.setOpenedBy(openedBy);
        cr.setCompany(company);
        cr.setOpenedAt(OffsetDateTime.now());
        return em.persistAndFlush(cr);
    }

    private Shift persistShift(CashRegister cashRegister, User seller, ShiftStatus status, Company company) {
        Shift shift = new Shift();
        shift.setId(UUID.randomUUID());
        shift.setCashRegister(cashRegister);
        shift.setSeller(seller);
        shift.setInitialCash(new BigDecimal("500.00"));
        shift.setStatus(status);
        shift.setCompany(company);
        shift.setStartedAt(OffsetDateTime.now());
        return em.persistAndFlush(shift);
    }

    @Test
    void existsByBusinessDateAndStatusScopedToCompany() {
        Company companyA = persistCompany("Company A");
        Company companyB = persistCompany("Company B");
        User userA = persistUser("userA", companyA);
        User userB = persistUser("userB", companyB);

        LocalDate today = LocalDate.now();
        persistCashRegister(today, CashRegisterStatus.OPEN, userA, companyA);

        assertTrue(cashRegisterRepository.existsByBusinessDateAndStatus(today, CashRegisterStatus.OPEN, companyA.getId()));
        assertFalse(cashRegisterRepository.existsByBusinessDateAndStatus(today, CashRegisterStatus.OPEN, companyB.getId()));
    }

    @Test
    void findFirstOpenCashRegisterScopedToCompany() {
        Company companyA = persistCompany("Company A");
        Company companyB = persistCompany("Company B");
        User userA = persistUser("userA", companyA);
        User userB = persistUser("userB", companyB);

        LocalDate today = LocalDate.now();
        persistCashRegister(today, CashRegisterStatus.OPEN, userA, companyA);
        persistCashRegister(today, CashRegisterStatus.OPEN, userB, companyB);

        Optional<CashRegister> resultA = cashRegisterRepository
                .findFirstByBusinessDateAndStatusOrderByOpenedAtDesc(today, CashRegisterStatus.OPEN, companyA.getId());
        assertTrue(resultA.isPresent());
        assertEquals(companyA.getId(), resultA.get().getCompany().getId());

        Optional<CashRegister> resultB = cashRegisterRepository
                .findFirstByBusinessDateAndStatusOrderByOpenedAtDesc(today, CashRegisterStatus.OPEN, companyB.getId());
        assertTrue(resultB.isPresent());
        assertEquals(companyB.getId(), resultB.get().getCompany().getId());
    }

    @Test
    void findAllByBusinessDateScopedToCompany() {
        Company companyA = persistCompany("Company A");
        Company companyB = persistCompany("Company B");
        User userA = persistUser("userA", companyA);
        User userB = persistUser("userB", companyB);

        LocalDate today = LocalDate.now();
        persistCashRegister(today, CashRegisterStatus.OPEN, userA, companyA);
        persistCashRegister(today, CashRegisterStatus.CLOSED, userB, companyB);

        List<CashRegister> resultA = cashRegisterRepository.findAllByBusinessDateOrderByOpenedAtDesc(today, companyA.getId());
        assertEquals(1, resultA.size());
        assertEquals(companyA.getId(), resultA.get(0).getCompany().getId());

        List<CashRegister> resultB = cashRegisterRepository.findAllByBusinessDateOrderByOpenedAtDesc(today, companyB.getId());
        assertEquals(1, resultB.size());
        assertEquals(companyB.getId(), resultB.get(0).getCompany().getId());
    }

    @Test
    void existsByIdAndCompanyIdValidatesOwnership() {
        Company companyA = persistCompany("Company A");
        Company companyB = persistCompany("Company B");
        User userA = persistUser("userA", companyA);

        CashRegister cr = persistCashRegister(LocalDate.now(), CashRegisterStatus.OPEN, userA, companyA);

        assertTrue(cashRegisterRepository.existsByIdAndCompanyId(cr.getId(), companyA.getId()));
        assertFalse(cashRegisterRepository.existsByIdAndCompanyId(cr.getId(), companyB.getId()));
    }

    @Test
    void findBySellerIdAndStatusScopedToCompany() {
        Company companyA = persistCompany("Company A");
        Company companyB = persistCompany("Company B");
        User userA = persistUser("userA", companyA);
        User userB = persistUser("userB", companyB);

        LocalDate today = LocalDate.now();
        CashRegister crA = persistCashRegister(today, CashRegisterStatus.OPEN, userA, companyA);
        CashRegister crB = persistCashRegister(today, CashRegisterStatus.OPEN, userB, companyB);

        persistShift(crA, userA, ShiftStatus.OPEN, companyA);
        persistShift(crB, userB, ShiftStatus.OPEN, companyB);

        // User A should find their shift, not company B's
        Optional<Shift> shiftA = shiftRepository.findBySellerIdAndStatus(userA.getId(), ShiftStatus.OPEN, companyA.getId());
        assertTrue(shiftA.isPresent());
        assertEquals(companyA.getId(), shiftA.get().getCompany().getId());

        // User B should find their shift
        Optional<Shift> shiftB = shiftRepository.findBySellerIdAndStatus(userB.getId(), ShiftStatus.OPEN, companyB.getId());
        assertTrue(shiftB.isPresent());
        assertEquals(companyB.getId(), shiftB.get().getCompany().getId());

        // User B should NOT find user A's shift even with correct sellerId
        Optional<Shift> crossCompany = shiftRepository.findBySellerIdAndStatus(userA.getId(), ShiftStatus.OPEN, companyB.getId());
        assertFalse(crossCompany.isPresent());
    }

    @Test
    void existsByIdAndCompanyIdForShift() {
        Company companyA = persistCompany("Company A");
        Company companyB = persistCompany("Company B");
        User userA = persistUser("userA", companyA);

        CashRegister cr = persistCashRegister(LocalDate.now(), CashRegisterStatus.OPEN, userA, companyA);
        Shift shift = persistShift(cr, userA, ShiftStatus.OPEN, companyA);

        assertTrue(shiftRepository.existsByIdAndCompanyId(shift.getId(), companyA.getId()));
        assertFalse(shiftRepository.existsByIdAndCompanyId(shift.getId(), companyB.getId()));
    }

    @Test
    void findShiftsByBusinessDateScopedToCompany() {
        Company companyA = persistCompany("Company A");
        Company companyB = persistCompany("Company B");
        User userA = persistUser("userA", companyA);
        User userB = persistUser("userB", companyB);

        LocalDate today = LocalDate.now();
        CashRegister crA = persistCashRegister(today, CashRegisterStatus.OPEN, userA, companyA);
        CashRegister crB = persistCashRegister(today, CashRegisterStatus.OPEN, userB, companyB);

        persistShift(crA, userA, ShiftStatus.OPEN, companyA);
        persistShift(crB, userB, ShiftStatus.OPEN, companyB);
        persistShift(crA, userA, ShiftStatus.CLOSED, companyA);

        List<Shift> shiftsA = shiftRepository.findByCashRegisterBusinessDateDesc(today, companyA.getId());
        assertEquals(2, shiftsA.size());

        List<Shift> shiftsB = shiftRepository.findByCashRegisterBusinessDateDesc(today, companyB.getId());
        assertEquals(1, shiftsB.size());
    }
}
