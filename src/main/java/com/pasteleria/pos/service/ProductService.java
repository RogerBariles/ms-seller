package com.pasteleria.pos.service;

import com.pasteleria.pos.config.AppConstants;
import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.domain.entity.Product;
import com.pasteleria.pos.domain.entity.ProductPriceAudit;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.PriceChangeType;
import com.pasteleria.pos.domain.enums.PriceField;
import com.pasteleria.pos.domain.enums.ProductCategory;
import com.pasteleria.pos.domain.enums.UserRole;
import com.pasteleria.pos.dto.BulkPriceIncreaseRequest;
import com.pasteleria.pos.dto.ProductPriceAuditResponse;
import com.pasteleria.pos.dto.ProductRequest;
import com.pasteleria.pos.dto.ProductResponse;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.mapper.DtoMapper;
import com.pasteleria.pos.repository.ProductPriceAuditRepository;
import com.pasteleria.pos.repository.ProductRepository;
import com.pasteleria.pos.repository.SaleItemRepository;
import com.pasteleria.pos.security.SecurityUtils;
import com.pasteleria.pos.security.UserPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductPriceAuditRepository auditRepository;
    private final SaleItemRepository saleItemRepository;
    private final UserService userService;
    private final CompanyService companyService;

    public ProductService(
            ProductRepository productRepository,
            ProductPriceAuditRepository auditRepository,
            SaleItemRepository saleItemRepository,
            UserService userService,
            CompanyService companyService) {
        this.productRepository = productRepository;
        this.auditRepository = auditRepository;
        this.saleItemRepository = saleItemRepository;
        this.userService = userService;
        this.companyService = companyService;
    }

    public List<ProductResponse> listProducts() {
        return productRepository.findAllWithCompany().stream()
                .map(DtoMapper::toProductResponse)
                .toList();
    }

    public List<ProductResponse> search(String q, ProductCategory category) {
        String query = (q == null || q.isBlank()) ? null : q.trim().toLowerCase();
        String pattern = query == null ? null : "%" + query + "%";

        UserPrincipal principal = SecurityUtils.currentUser();
        if (principal.getRole() == UserRole.SELLER) {
            User user = userService.getUserEntity(principal.getId());
            Company userCompany = user.getCompany();
            UUID companyId = userCompany != null ? userCompany.getId() : null;
            if (companyId != null) {
                return productRepository.searchByCompany(pattern, category, companyId).stream()
                        .map(DtoMapper::toProductResponse)
                        .toList();
            }
        }
        return productRepository.search(pattern, category).stream()
                .map(DtoMapper::toProductResponse)
                .toList();
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName(request.name().trim());
        product.setCategory(request.category());
        product.setPrice(request.price());
        product.setPurchasePrice(request.purchasePrice());
        product.setActive(request.active());
        product.setCompany(resolveCompany(request.companyId()));
        return DtoMapper.toProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

        if (product.getPrice().compareTo(request.price()) != 0) {
            auditPriceChange(product, product.getPrice(), request.price(), PriceField.SALE, PriceChangeType.INDIVIDUAL);
        }
        if (product.getPurchasePrice().compareTo(request.purchasePrice()) != 0) {
            auditPriceChange(
                    product,
                    product.getPurchasePrice(),
                    request.purchasePrice(),
                    PriceField.PURCHASE,
                    PriceChangeType.INDIVIDUAL);
        }

        product.setName(request.name().trim());
        product.setCategory(request.category());
        product.setPrice(request.price());
        product.setPurchasePrice(request.purchasePrice());
        product.setActive(request.active());
        product.setCompany(resolveCompany(request.companyId()));
        product.setUpdatedAt(OffsetDateTime.now());
        return DtoMapper.toProductResponse(productRepository.save(product));
    }

    @Transactional
    public int bulkPriceIncrease(BulkPriceIncreaseRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        User user = userService.getUserEntity(principal.getId());
        List<Product> products = productRepository.findByActiveTrue();
        int updated = 0;

        for (Product product : products) {
            boolean changed = false;
            if (request.target() == PriceField.SALE || request.target() == PriceField.BOTH) {
                changed |= applyBulkIncrease(product, user, product.getPrice(), PriceField.SALE, request.percentage());
            }
            if (request.target() == PriceField.PURCHASE || request.target() == PriceField.BOTH) {
                changed |= applyBulkIncrease(
                        product, user, product.getPurchasePrice(), PriceField.PURCHASE, request.percentage());
            }
            if (changed) {
                product.setUpdatedAt(OffsetDateTime.now());
                productRepository.save(product);
                updated++;
            }
        }
        return updated;
    }

    public List<ProductPriceAuditResponse> getProductAudit(UUID productId) {
        return auditRepository.findByProductIdOrderByChangedAtDesc(productId).stream()
                .map(DtoMapper::toAuditResponse)
                .toList();
    }

    public Product getActiveProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        if (!product.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El producto no está activo");
        }
        return product;
    }

    public Product getProductEntity(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    private Company resolveCompany(UUID companyId) {
        Company company = companyService.getCompanyEntity(companyId);
        if (!company.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La empresa seleccionada no está activa");
        }
        return company;
    }

    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        if (id.equals(AppConstants.BIRTHDAY_PRODUCT_ID)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No se puede eliminar el producto de cumpleaños");
        }
        long salesCount = saleItemRepository.countByProductId(id);
        if (salesCount > 0) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "No se puede eliminar: el producto tiene ventas registradas. Desactívelo en su lugar.");
        }
        auditRepository.deleteAll(auditRepository.findByProductIdOrderByChangedAtDesc(id));
        productRepository.delete(product);
    }

    private boolean applyBulkIncrease(
            Product product,
            User user,
            BigDecimal currentPrice,
            PriceField field,
            BigDecimal percentage) {
        BigDecimal newPrice = currentPrice.multiply(
                        BigDecimal.ONE.add(percentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);
        if (currentPrice.compareTo(newPrice) == 0) {
            return false;
        }
        saveAudit(product, user, currentPrice, newPrice, PriceChangeType.BULK_PERCENTAGE, field);
        if (field == PriceField.SALE) {
            product.setPrice(newPrice);
        } else {
            product.setPurchasePrice(newPrice);
        }
        return true;
    }

    private void auditPriceChange(
            Product product,
            BigDecimal oldPrice,
            BigDecimal newPrice,
            PriceField field,
            PriceChangeType changeType) {
        UserPrincipal principal = SecurityUtils.currentUser();
        User user = userService.getUserEntity(principal.getId());
        saveAudit(product, user, oldPrice, newPrice, changeType, field);
    }

    private void saveAudit(
            Product product,
            User user,
            BigDecimal oldPrice,
            BigDecimal newPrice,
            PriceChangeType changeType,
            PriceField priceField) {
        ProductPriceAudit audit = new ProductPriceAudit();
        audit.setId(UUID.randomUUID());
        audit.setProduct(product);
        audit.setChangedBy(user);
        audit.setOldPrice(oldPrice);
        audit.setNewPrice(newPrice);
        audit.setChangeType(changeType);
        audit.setPriceField(priceField);
        audit.setChangedAt(OffsetDateTime.now());
        auditRepository.save(audit);
    }
}
