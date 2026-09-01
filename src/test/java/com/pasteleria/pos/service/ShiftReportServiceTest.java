package com.pasteleria.pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.domain.entity.Shift;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.ShiftStatus;
import com.pasteleria.pos.domain.enums.UserRole;
import com.pasteleria.pos.dto.ShiftHoursReportResponse;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.repository.ShiftRepository;
import com.pasteleria.pos.repository.UserRepository;
import com.pasteleria.pos.security.SecurityUtils;
import com.pasteleria.pos.security.UserPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

class ShiftReportServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID OTHER_COMPANY_ID = UUID.randomUUID();
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-01T21:00:00Z");

    private ShiftRepository shiftRepository;
    private UserRepository userRepository;
    private ShiftReportService shiftReportService;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        shiftRepository = mock(ShiftRepository.class);
        userRepository = mock(UserRepository.class);
        Clock clock = Clock.fixed(FIXED_INSTANT, ZONE);
        shiftReportService = new ShiftReportService(shiftRepository, userRepository, clock);
        securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::currentUser)
                .thenReturn(new UserPrincipal(createUser(UUID.randomUUID(), UserRole.SUPER_ADMIN, COMPANY_ID)));
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    @Test
    void sumsClosedShiftAndOpenShiftUsingNow() {
        User seller = createUser(SELLER_ID, UserRole.SELLER, COMPANY_ID);
        when(userRepository.findByIdWithCompany(SELLER_ID)).thenReturn(Optional.of(seller));

        Shift closed = createShift(
                UUID.randomUUID(),
                seller,
                ShiftStatus.CLOSED,
                OffsetDateTime.parse("2026-09-01T08:00:00-03:00"),
                OffsetDateTime.parse("2026-09-01T12:30:00-03:00"));
        Shift open = createShift(
                UUID.randomUUID(),
                seller,
                ShiftStatus.OPEN,
                OffsetDateTime.parse("2026-09-01T16:00:00-03:00"),
                null);
        when(shiftRepository.findBySellerAndCompanyAndStartedAtBetween(eq(SELLER_ID), eq(COMPANY_ID), any(), any()))
                .thenReturn(List.of(open, closed));

        ShiftHoursReportResponse report = shiftReportService.getShiftHoursReport(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1), SELLER_ID);

        assertEquals(SELLER_ID, report.sellerId());
        assertEquals(seller.getName(), report.sellerName());
        assertEquals(2, report.shifts().size());
        assertEquals(120, report.shifts().get(0).durationMinutes());
        assertEquals(270, report.shifts().get(1).durationMinutes());
        assertEquals(390, report.totalDurationMinutes());
        verify(shiftRepository).findBySellerAndCompanyAndStartedAtBetween(
                eq(SELLER_ID), eq(COMPANY_ID), any(), any());
    }

    @Test
    void rejectsSellerFromAnotherCompany() {
        User seller = createUser(SELLER_ID, UserRole.SELLER, OTHER_COMPANY_ID);
        when(userRepository.findByIdWithCompany(SELLER_ID)).thenReturn(Optional.of(seller));

        ApiException ex = assertThrows(ApiException.class, () ->
                shiftReportService.getShiftHoursReport(
                        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1), SELLER_ID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("La vendedora no pertenece a la empresa", ex.getMessage());
    }

    private static User createUser(UUID id, UserRole role, UUID companyId) {
        User user = new User();
        user.setId(id);
        user.setName(role.name());
        user.setUsername(role.name().toLowerCase() + "-" + id);
        user.setPasswordHash("hash");
        user.setRole(role);
        user.setActive(true);
        Company company = new Company();
        company.setId(companyId);
        company.setName("Company");
        company.setActive(true);
        user.setCompany(company);
        return user;
    }

    private static Shift createShift(
            UUID id,
            User seller,
            ShiftStatus status,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt) {
        Shift shift = new Shift();
        shift.setId(id);
        shift.setSeller(seller);
        shift.setCompany(seller.getCompany());
        shift.setStatus(status);
        shift.setStartedAt(startedAt);
        shift.setEndedAt(endedAt);
        return shift;
    }
}
