package com.pasteleria.pos.mapper;

import com.pasteleria.pos.domain.entity.CashRegister;
import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.domain.entity.Product;
import com.pasteleria.pos.domain.entity.ProductPriceAudit;
import com.pasteleria.pos.domain.entity.Sale;
import com.pasteleria.pos.domain.entity.SaleItem;
import com.pasteleria.pos.domain.entity.Shift;
import com.pasteleria.pos.domain.entity.ShiftCashMovement;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.dto.CashRegisterResponse;
import com.pasteleria.pos.dto.CompanyResponse;
import com.pasteleria.pos.dto.ProductPriceAuditResponse;
import com.pasteleria.pos.dto.ProductResponse;
import com.pasteleria.pos.dto.SaleItemResponse;
import com.pasteleria.pos.dto.SaleResponse;
import com.pasteleria.pos.dto.ShiftCashMovementResponse;
import com.pasteleria.pos.dto.ShiftResponse;
import com.pasteleria.pos.dto.UserResponse;
import java.util.List;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static UserResponse toUserResponse(User user) {
        Company company = user.getCompany();
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getRole(),
                user.isActive(),
                user.getBirthDate(),
                company != null ? company.getId() : null,
                company != null ? company.getName() : null);
    }

    public static CompanyResponse toCompanyResponse(Company company) {
        return new CompanyResponse(company.getId(), company.getName(), company.getDetail(), company.isActive());
    }

    public static CashRegisterResponse toCashRegisterResponse(CashRegister cashRegister) {
        return new CashRegisterResponse(
                cashRegister.getId(),
                cashRegister.getBusinessDate(),
                cashRegister.getInitialCash(),
                cashRegister.getStatus(),
                cashRegister.getOpenedBy().getId(),
                cashRegister.getOpenedBy().getName(),
                cashRegister.getOpenedAt(),
                cashRegister.getClosedAt());
    }

    public static ShiftResponse toShiftResponse(Shift shift) {
        return new ShiftResponse(
                shift.getId(),
                shift.getCashRegister().getId(),
                shift.getSeller().getId(),
                shift.getSeller().getName(),
                shift.getInitialCash(),
                shift.getStatus(),
                shift.getStartedAt(),
                shift.getEndedAt());
    }

    public static ShiftCashMovementResponse toShiftCashMovementResponse(ShiftCashMovement movement) {
        return new ShiftCashMovementResponse(
                movement.getId(),
                movement.getMovementType(),
                movement.getAmount(),
                movement.getDetail(),
                movement.getCreatedBy().getName(),
                movement.getCreatedAt());
    }

    public static ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.getPurchasePrice(),
                product.isActive(),
                product.getUpdatedAt());
    }

    public static ProductPriceAuditResponse toAuditResponse(ProductPriceAudit audit) {
        return new ProductPriceAuditResponse(
                audit.getId(),
                audit.getProduct().getId(),
                audit.getProduct().getName(),
                audit.getChangedBy().getId(),
                audit.getChangedBy().getName(),
                audit.getOldPrice(),
                audit.getNewPrice(),
                audit.getPriceField(),
                audit.getChangeType(),
                audit.getChangedAt());
    }

    public static SaleResponse toSaleResponse(Sale sale) {
        List<SaleItemResponse> items = sale.getItems().stream()
                .map(DtoMapper::toSaleItemResponse)
                .toList();
        return new SaleResponse(
                sale.getId(),
                sale.getSeller().getId(),
                sale.getSeller().getName(),
                sale.getPaymentMethod(),
                sale.getInstallments(),
                sale.getSubtotal(),
                sale.getDiscountTotal(),
                sale.getTotal(),
                sale.getTotalDiscountType(),
                sale.getTotalDiscountValue(),
                sale.getCreatedAt(),
                items);
    }

    public static SaleItemResponse toSaleItemResponse(SaleItem item) {
        return new SaleItemResponse(
                item.getProduct().getId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getDiscountType(),
                item.getDiscountValue(),
                item.getLineSubtotal(),
                item.getLineDiscount(),
                item.getLineTotal());
    }
}
