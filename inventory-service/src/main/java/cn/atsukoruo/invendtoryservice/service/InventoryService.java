package cn.atsukoruo.invendtoryservice.service;

import cn.atsukoruo.invendtoryservice.repository.InventoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    private final InventoryMapper inventoryMapper;
    public InventoryService(InventoryMapper inventoryMapper) {
        this.inventoryMapper = inventoryMapper;
    }

    @Transactional
    public void createInventory(Integer productId, Integer amount) {
        inventoryMapper.createInventory(productId, amount);
    }

    @Transactional
    public boolean updateInventory(Integer productId, Integer diff) {
        Integer remaining = inventoryMapper.queryInventory(productId);
        if (remaining < diff) {
            return false;
        }
        inventoryMapper.updateInventory(productId, diff);
        return true;
    }
}
