package com.devsam.urbantrend.Roles;

import com.devsam.urbantrend.Enum.Permission;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Set;

public enum UserRole {

    ADMIN(Set.of(Permission.values())), // Admin gets everything

    MANAGER(Set.of(
            // Leads
            Permission.CAN_VIEW_LEADS,
            Permission.CAN_UPDATE_ORDER,
            Permission.CAN_CREATE_LEADS,
            Permission.CAN_UPDATE_LEAD_STATUS,
            Permission.CAN_CONVERT_LEADS,
            // Orders
            Permission.CAN_VIEW_ORDERS,
            Permission.CAN_CREATE_ORDERS,
            Permission.CAN_UPDATE_ORDER_STATUS,
            Permission.CAN_VIEW_ORDER_SUMMARY,
            Permission.CAN_VIEW_TOP_SELLING,
            // Products
            Permission.CAN_VIEW_PRODUCTS,
            Permission.CAN_CREATE_PRODUCTS,
            Permission.CAN_EDIT_PRODUCTS,
            Permission.CAN_DELETE_PRODUCTS,
            // Categories
            Permission.CAN_VIEW_CATEGORIES,
            Permission.CAN_CREATE_CATEGORIES,
            Permission.CAN_EDIT_CATEGORIES,
            Permission.CAN_DELETE_CATEGORIES,
            // Customers
            Permission.CAN_VIEW_CUSTOMERS,
            Permission.CAN_CREATE_CUSTOMERS,
            Permission.CAN_DELETE_CUSTOMERS,
            // Inventory
            Permission.CAN_VIEW_INVENTORY,
            Permission.CAN_VIEW_LOW_STOCK,
            Permission.CAN_RESTOCK_INVENTORY,
            Permission.CAN_REDUCE_INVENTORY,
            // Payments
            Permission.CAN_VIEW_PAYMENTS,
            Permission.CAN_CREATE_PAYMENTS,
            Permission.CAN_MATCH_PAYMENTS,
            Permission.CAN_VIEW_ORDER_PAYMENTS,
            // Requisitions
            Permission.CAN_VIEW_REQUISITIONS,
            Permission.CAN_CREATE_REQUISITIONS,
            Permission.CAN_VIEW_OWN_REQUISITIONS,
            Permission.CAN_APPROVE_REQUISITIONS,
            Permission.CAN_REJECT_REQUISITIONS,
            Permission.CAN_MARK_REQUISITION_PAID,
            // Expenses
            Permission.CAN_VIEW_EXPENSES,
            Permission.CAN_CREATE_EXPENSES,
            Permission.CAN_DELETE_EXPENSES,
            // Payroll
            Permission.CAN_VIEW_OWN_PAYROLL,
            Permission.CAN_VIEW_PAYROLL_SUMMARY,
            // Reports
            Permission.CAN_VIEW_PROFIT_REPORT,
            // Staff
            Permission.CAN_VIEW_STAFF
    )),

    STAFF(Set.of(
            Permission.CAN_VIEW_LEADS,
            Permission.CAN_CREATE_LEADS,
            Permission.CAN_UPDATE_LEAD_STATUS,
            Permission.CAN_CONVERT_LEADS,
            Permission.CAN_VIEW_ORDERS,
            Permission.CAN_CREATE_ORDERS,
            Permission.CAN_UPDATE_ORDER_STATUS,
            Permission.CAN_VIEW_ORDER_SUMMARY,
            Permission.CAN_VIEW_PRODUCTS,
            Permission.CAN_VIEW_CATEGORIES,
            Permission.CAN_VIEW_CUSTOMERS,
            Permission.CAN_CREATE_CUSTOMERS,
            Permission.CAN_VIEW_REQUISITIONS,
            Permission.CAN_CREATE_REQUISITIONS,
            Permission.CAN_VIEW_OWN_REQUISITIONS,
            Permission.CAN_VIEW_OWN_PAYROLL
    )),

    CASHIER(Set.of(
            Permission.CAN_VIEW_ORDERS,
            Permission.CAN_CREATE_ORDERS,
            Permission.CAN_VIEW_PRODUCTS,
            Permission.CAN_VIEW_CATEGORIES,
            Permission.CAN_VIEW_CUSTOMERS,
            Permission.CAN_CREATE_CUSTOMERS,
            Permission.CAN_VIEW_PAYMENTS,
            Permission.CAN_CREATE_PAYMENTS,
            Permission.CAN_MATCH_PAYMENTS,
            Permission.CAN_VIEW_ORDER_PAYMENTS,
            Permission.CAN_VIEW_REQUISITIONS,
            Permission.CAN_CREATE_REQUISITIONS,
            Permission.CAN_VIEW_OWN_REQUISITIONS,
            Permission.CAN_VIEW_OWN_PAYROLL
    )),

    ATTENDANT(Set.of(

    ));

    private final Set<Permission> permissions;

    UserRole(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    @JsonCreator
    public static UserRole fromString(String key) {
        return UserRole.valueOf(key.toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}