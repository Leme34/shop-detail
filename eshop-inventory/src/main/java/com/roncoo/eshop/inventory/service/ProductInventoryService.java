package com.roncoo.eshop.inventory.service;

import com.roncoo.eshop.inventory.model.ProductInventory;

/**
 * 商品库存Service接口
 * @author Administrator
 *
 */
public interface ProductInventoryService {

	/**
	 * 更新商品库存
	 * @param productInventory 商品库存
	 */
	void updateProductInventory(ProductInventory productInventory);

	/**
	 * 删除Redis中的商品库存的缓存
	 * @param productInventory 商品库存
	 */
	void removeProductInventoryCache(ProductInventory productInventory);

	/**
	 * 根据商品id查询商品库存
	 * @param productId 商品id
	 * @return 商品库存
	 */
	ProductInventory findProductInventory(Integer productId);

	/**
	 * 设置商品库存的缓存
	 * @param productInventory 商品库存
	 */
	void setProductInventoryCache(ProductInventory productInventory);

	/**
	 * 从缓存中获取商品库存
	 * @param productId 商品id
	 * @return 命中缓存则返回库存数，否则返回null
	 */
	ProductInventory getProductInventoryCache(Integer productId);

}
