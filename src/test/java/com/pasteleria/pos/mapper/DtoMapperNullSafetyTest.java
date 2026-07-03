package com.pasteleria.pos.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.pasteleria.pos.domain.entity.CashRegister;
import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.domain.entity.Shift;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.CashRegisterStatus;
import com.pasteleria.pos.domain.enums.ShiftStatus;
import com.pasteleria.pos.dto.CashRegisterResponse;
import com.pasteleria.pos.dto.ShiftResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DtoMapperNullSafetyTest {

    @Test
    void toCashRegisterResponseWithCompany() {
        UUID companyId = UUID.randomUUID();
        Company company = new Company();
        company.setId(companyId);
        company.setName("Test Company");

        CashRegister cr = createCashRegister();
        cr.setCompany(company);

        CashRegisterResponse response = DtoMapper.toCashRegisterResponse(cr);

        assertNotNull(response);
        assertEquals(companyId, response.companyId());
        assertEquals("Test Company", response.companyName());
    }

    @Test
    void toCashRegisterResponseWithNullCompany() {
        CashRegister cr = createCashRegister();
        cr.setCompany(null);

        CashRegisterResponse response = DtoMapper.toCashRegisterResponse(cr);

        assertNotNull(response);
        assertNull(response.companyId());
        assertNull(response.companyName());
    }

    @Test
    void toShiftResponseWithCompany() {
        UUID companyId = UUID.randomUUID();
        Company company = new Company();
        company.setId(companyId);
        company.setName("Test Company");

        Shift shift = createShift();
        shift.setCompany(company);

        ShiftResponse response = DtoMapper.toShiftResponse(shift);

        assertNotNull(response);
        assertEquals(companyId, response.companyId());
        assertEquals("Test Company", response.companyName());
    }

    @Test
    void toShiftResponseWithNullCompany() {
        Shift shift = createShift();
        shift.setCompany(null);

        ShiftResponse response = DtoMapper.toShiftResponse(shift);

        assertNotNull(response);
        assertNull(response.companyId());
        assertNull(response.companyName());
    }

    private static CashRegister createCashRegister() {
        User openedBy = new User();
        openedBy.setId(UUID.randomUUID());
        openedBy.setName("Opener");

        CashRegister cr = new CashRegister();
        cr.setId(UUID.randomUUID());
        cr.setBusinessDate(LocalDate.now());
        cr.setInitialCash(new BigDecimal("1000.00"));
        cr.setStatus(CashRegisterStatus.OPEN);
        cr.setOpenedBy(openedBy);
        cr.setOpenedAt(OffsetDateTime.now());
        return cr;
    }

    private static Shift createShift() {
        User seller = new User();
        seller.setId(UUID.randomUUID());
        seller.setName("Seller");

        CashRegister cr = new CashRegister();
        cr.setId(UUID.randomUUID());

        Shift shift = new Shift();
        shift.setId(UUID.randomUUID());
        shift.setCashRegister(cr);
        shift.setSeller(seller);
        shift.setInitialCash(new BigDecimal("500.00"));
        shift.setStatus(ShiftStatus.OPEN);
        shift.setStartedAt(OffsetDateTime.now());
        return shift;
    }
}
