package com.pasteleria.pos.domain.enums;

public enum CashMovementCategory {
    SUPPLIER_PAYMENT,
    EXPENSE,
    BANK_DEPOSIT,
    OWNER_WITHDRAWAL,
    CHANGE_FUND,
    OTHER;

    public boolean allowedFor(CashMovementType type) {
        return switch (this) {
            case SUPPLIER_PAYMENT, EXPENSE, BANK_DEPOSIT, OWNER_WITHDRAWAL -> type == CashMovementType.WITHDRAWAL;
            case CHANGE_FUND -> type == CashMovementType.INCOME;
            case OTHER -> true;
        };
    }

    /** El movimiento genera un egreso en Contabilidad. */
    public boolean createsExpense() {
        return this == SUPPLIER_PAYMENT || this == EXPENSE;
    }

    /**
     * Si cuenta en el monto final: los que generan egreso ya restan por ese lado,
     * y depósitos o cambio solo mueven plata de lugar.
     */
    public boolean affectsResult() {
        return this == OWNER_WITHDRAWAL || this == OTHER;
    }
}
