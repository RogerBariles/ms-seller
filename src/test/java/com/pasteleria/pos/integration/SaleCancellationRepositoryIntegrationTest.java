package com.pasteleria.pos.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pasteleria.pos.domain.entity.CashRegister;
import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.domain.entity.Sale;
import com.pasteleria.pos.domain.entity.Shift;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.CashRegisterStatus;
import com.pasteleria.pos.domain.enums.PaymentMethod;
import com.pasteleria.pos.domain.enums.SaleStatus;
import com.pasteleria.pos.domain.enums.ShiftStatus;
import com.pasteleria.pos.domain.enums.UserRole;
import com.pasteleria.pos.repository.SaleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

/** Las anuladas no cuentan como venta, pero su efectivo sí entró a la caja (la devolución es un retiro aparte). */
@DataJpaTest
@ActiveProfiles("test")
class SaleCancellationRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SaleRepository saleRepository;

    private Company company;
    private User seller;
    private Shift shift;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(UUID.randomUUID());
        company.setName("Centro");
        company.setActive(true);
        em.persist(company);

        seller = new User();
        seller.setId(UUID.randomUUID());
        seller.setName("Vendedora");
        seller.setUsername("vendedora");
        seller.setPasswordHash("pwd");
        seller.setRole(UserRole.SELLER);
        seller.setActive(true);
        seller.setCompany(company);
        em.persist(seller);

        CashRegister cashRegister = new CashRegister();
        cashRegister.setId(UUID.randomUUID());
        cashRegister.setBusinessDate(LocalDate.now());
        cashRegister.setInitialCash(new BigDecimal("1000.00"));
        cashRegister.setStatus(CashRegisterStatus.OPEN);
        cashRegister.setOpenedBy(seller);
        cashRegister.setCompany(company);
        cashRegister.setOpenedAt(OffsetDateTime.now());
        em.persist(cashRegister);

        shift = new Shift();
        shift.setId(UUID.randomUUID());
        shift.setCashRegister(cashRegister);
        shift.setSeller(seller);
        shift.setInitialCash(new BigDecimal("1000.00"));
        shift.setStatus(ShiftStatus.OPEN);
        shift.setCompany(company);
        shift.setStartedAt(OffsetDateTime.now());
        em.persist(shift);

        persistSale(PaymentMethod.EFECTIVO, "1000.00", "1000.00", SaleStatus.SOLD);
        persistSale(PaymentMethod.EFECTIVO, "500.00", "500.00", SaleStatus.CANCELLED);
        persistSale(PaymentMethod.TARJETA, "2000.00", "0.00", SaleStatus.SOLD);
        persistSale(PaymentMethod.TARJETA, "700.00", "0.00", SaleStatus.CANCELLED);
        em.flush();
    }

    @Test
    void cancelledSalesAreNotCountedAsSales() {
        assertEquals(2, saleRepository.countByShiftId(shift.getId()));
        assertEquals(2, saleRepository.countByCashRegisterId(shift.getCashRegister().getId()));
        assertEquals(0, new BigDecimal("2000.00").compareTo(
                saleRepository.sumNonCashAmountByShiftAndPaymentMethod(shift.getId(), PaymentMethod.TARJETA)));
        OffsetDateTime from = OffsetDateTime.now().minusDays(1);
        OffsetDateTime to = OffsetDateTime.now().plusDays(1);
        assertEquals(0, new BigDecimal("3000.00").compareTo(
                saleRepository.sumSalesBetweenByCompany(from, to, company.getId())));
    }

    @Test
    void cashOfCancelledSalesStillEnteredTheRegister() {
        assertEquals(0, new BigDecimal("1500.00").compareTo(saleRepository.sumCashAmountByShift(shift.getId())));
        assertEquals(0, new BigDecimal("1500.00").compareTo(
                saleRepository.sumCashAmountByCashRegister(shift.getCashRegister().getId())));
    }

    private void persistSale(PaymentMethod method, String total, String cash, SaleStatus status) {
        Sale sale = new Sale();
        sale.setId(UUID.randomUUID());
        sale.setShift(shift);
        sale.setSeller(seller);
        sale.setCompany(company);
        sale.setPaymentMethod(method);
        sale.setSubtotal(new BigDecimal(total));
        sale.setDiscountTotal(BigDecimal.ZERO);
        sale.setTotal(new BigDecimal(total));
        sale.setCashAmount(new BigDecimal(cash));
        sale.setAccreditedAt(LocalDate.now());
        sale.setStatus(status);
        em.persist(sale);
    }
}
