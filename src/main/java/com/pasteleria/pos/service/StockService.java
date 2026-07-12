package com.pasteleria.pos.service;

import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.domain.entity.Product;
import com.pasteleria.pos.domain.entity.StockMovement;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.StockMovementType;
import com.pasteleria.pos.domain.enums.StockReferenceType;
import com.pasteleria.pos.dto.StockAdjustRequest;
import com.pasteleria.pos.dto.StockPurchaseRequest;
import com.pasteleria.pos.dto.StockResponse;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.mapper.DtoMapper;
import com.pasteleria.pos.repository.ProductRepository;
import com.pasteleria.pos.repository.StockMovementRepository;
import com.pasteleria.pos.security.SecurityUtils;
import com.pasteleria.pos.security.UserPrincipal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final UserService userService;

    public StockService(
            StockMovementRepository stockMovementRepository,
            ProductRepository productRepository,
            UserService userService) {
        this.stockMovementRepository = stockMovementRepository;
        this.productRepository = productRepository;
        this.userService = userService;
    }

    public StockResponse getCurrentStock(UUID productId) {
        UserPrincipal principal = SecurityUtils.currentUser();
        User user = userService.getUserEntity(principal.getId());
        Product product = getProductCheckedByCompany(productId, user);
        return DtoMapper.toStockResponse(product);
    }

    @Transactional
    public StockResponse adjustStock(StockAdjustRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        User user = userService.getUserEntity(principal.getId());
        Company userCompany = user.getCompany();

        Product product = getProductCheckedByCompany(request.productId(), user);

        int newStock = product.getCurrentStock() != null ? product.getCurrentStock() + request.quantityChange() : request.quantityChange();
        product.setCurrentStock(newStock);
        productRepository.save(product);

        StockMovement movement = new StockMovement();
        movement.setId(UUID.randomUUID());
        movement.setProduct(product);
        movement.setQuantityChange(request.quantityChange());
        movement.setType(StockMovementType.ADJUSTMENT);
        movement.setReferenceType(StockReferenceType.ADJUSTMENT);
        movement.setNotes(request.notes());
        movement.setCompany(userCompany);
        movement.setCreatedBy(user);
        movement.setCreatedAt(OffsetDateTime.now());
        stockMovementRepository.save(movement);

        return DtoMapper.toStockResponse(product);
    }

    @Transactional
    public StockResponse recordPurchase(StockPurchaseRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        User user = userService.getUserEntity(principal.getId());
        Company userCompany = user.getCompany();

        Product product = getProductCheckedByCompany(request.productId(), user);

        int newStock = product.getCurrentStock() != null ? product.getCurrentStock() + request.quantity() : request.quantity();
        product.setCurrentStock(newStock);
        productRepository.save(product);

        StockMovement movement = new StockMovement();
        movement.setId(UUID.randomUUID());
        movement.setProduct(product);
        movement.setQuantityChange(request.quantity());
        movement.setType(StockMovementType.IN);
        movement.setReferenceType(StockReferenceType.PURCHASE);
        movement.setNotes(request.notes());
        movement.setCompany(userCompany);
        movement.setCreatedBy(user);
        movement.setCreatedAt(OffsetDateTime.now());
        stockMovementRepository.save(movement);

        return DtoMapper.toStockResponse(product);
    }

    @Transactional
    public void deductOnSale(UUID productId, int quantity, UUID saleId) {
        UserPrincipal principal = SecurityUtils.currentUser();
        User user = userService.getUserEntity(principal.getId());
        Company userCompany = user.getCompany();

        Product product = productRepository.findByIdAndCompanyId(productId, userCompany.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

        int newStock = product.getCurrentStock() != null ? product.getCurrentStock() - quantity : -quantity;
        product.setCurrentStock(newStock);
        productRepository.save(product);

        StockMovement movement = new StockMovement();
        movement.setId(UUID.randomUUID());
        movement.setProduct(product);
        movement.setQuantityChange(-quantity);
        movement.setType(StockMovementType.OUT);
        movement.setReferenceType(StockReferenceType.SALE);
        movement.setReferenceId(saleId);
        movement.setCompany(userCompany);
        movement.setCreatedBy(user);
        movement.setCreatedAt(OffsetDateTime.now());
        stockMovementRepository.save(movement);
    }

    private Product getProductCheckedByCompany(UUID productId, User user) {
        Company userCompany = user.getCompany();
        if (userCompany == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Usuario sin empresa asignada");
        }
        return productRepository.findByIdAndCompanyId(productId, userCompany.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado para esta empresa"));
    }
}
