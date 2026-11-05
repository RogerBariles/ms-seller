package com.pasteleria.pos.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.domain.entity.Sale;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.PaymentMethod;
import com.pasteleria.pos.domain.enums.UserRole;
import com.pasteleria.pos.dto.ReceivableResponse;
import com.pasteleria.pos.repository.SaleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class CashFlowRepositoryIntegrationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 5);
    private static final List<PaymentMethod> DELAYED = List.of(PaymentMethod.TARJETA, PaymentMethod.QR);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SaleRepository saleRepository;

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

    private void persistSale(
            User seller, PaymentMethod method, String total, String cash, LocalDate accreditedAt) {
        Sale sale = new Sale();
        sale.setId(UUID.randomUUID());
        sale.setSeller(seller);
        sale.setCompany(seller.getCompany());
        sale.setPaymentMethod(method);
        sale.setSubtotal(new BigDecimal(total));
        sale.setDiscountTotal(BigDecimal.ZERO);
        sale.setTotal(new BigDecimal(total));
        sale.setCashAmount(new BigDecimal(cash));
        sale.setAccreditedAt(accreditedAt);
        em.persistAndFlush(sale);
    }

    @Test
    void receivablesGroupNonCashAmountByDateAndMethod() {
        Company company = persistCompany("A");
        User seller = persistUser("a", company);
        persistSale(seller, PaymentMethod.TARJETA, "1000.00", "0.00", TODAY.plusDays(1));
        persistSale(seller, PaymentMethod.TARJETA, "500.00", "200.00", TODAY.plusDays(1));
        persistSale(seller, PaymentMethod.QR, "300.00", "0.00", TODAY.plusDays(2));

        List<ReceivableResponse> receivables =
                saleRepository.findReceivables(company.getId(), TODAY, TODAY.plusDays(7), DELAYED);

        assertEquals(2, receivables.size());
        assertEquals(TODAY.plusDays(1), receivables.get(0).date());
        assertEquals(PaymentMethod.TARJETA, receivables.get(0).paymentMethod());
        assertEquals(0, new BigDecimal("1300.00").compareTo(receivables.get(0).amount()));
        assertEquals(PaymentMethod.QR, receivables.get(1).paymentMethod());
    }

    @Test
    void receivablesSkipOtherCompaniesPastDatesAndFullyCashSales() {
        Company company = persistCompany("A");
        Company other = persistCompany("B");
        User seller = persistUser("a", company);
        persistSale(seller, PaymentMethod.TARJETA, "100.00", "0.00", TODAY.minusDays(1));
        persistSale(seller, PaymentMethod.TARJETA, "100.00", "0.00", TODAY.plusDays(8));
        persistSale(seller, PaymentMethod.TARJETA, "100.00", "100.00", TODAY.plusDays(1));
        persistSale(seller, PaymentMethod.TRANSFERENCIA, "100.00", "0.00", TODAY.plusDays(1));
        persistSale(persistUser("b", other), PaymentMethod.TARJETA, "100.00", "0.00", TODAY.plusDays(1));

        assertEquals(0, saleRepository.findReceivables(company.getId(), TODAY, TODAY.plusDays(7), DELAYED).size());
    }
}
