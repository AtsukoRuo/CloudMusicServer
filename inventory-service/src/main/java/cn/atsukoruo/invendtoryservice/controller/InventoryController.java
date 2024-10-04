package cn.atsukoruo.invendtoryservice.controller;

import cn.atsukoruo.common.utils.Response;
import cn.atsukoruo.invendtoryservice.service.InventoryService;
import org.springframework.web.bind.annotation.*;

@RestController
public class InventoryController {
    private final InventoryService inventoryService;
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("inventory")
    public Response<Object> createInventory(
            @RequestParam("productId") Integer productId,
            @RequestParam("amount") Integer amount
    ) {
        inventoryService.createInventory(productId, amount);
        return Response.success();
    }

    @PutMapping("inventory")
    public Response<Object> updateInventory(
            @RequestParam("productId") Integer productId,
            @RequestParam("diff") Integer diff
    ) {
        boolean canUpdate =  inventoryService.updateInventory(productId, diff);
        return Response.success(canUpdate);
    }
}
