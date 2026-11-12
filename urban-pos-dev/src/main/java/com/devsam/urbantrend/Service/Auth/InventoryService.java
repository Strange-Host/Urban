package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.InventoryRequest;
import com.devsam.urbantrend.Entity.Inventory;
import com.devsam.urbantrend.Entity.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface InventoryService {

    Inventory getInventoryByProduct(UUID productId);

    Inventory restock(InventoryRequest request, User user);

    Inventory reduce(InventoryRequest request);

    void reduceIfTracked(InventoryRequest request);

    void increaseIfTracked(InventoryRequest request);

    List<Inventory> getLowStock();
}
