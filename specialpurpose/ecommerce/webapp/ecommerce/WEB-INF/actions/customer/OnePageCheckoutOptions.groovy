import org.ofbiz.order.shoppingcart.shipping.*;

shoppingCart = session.getAttribute("shoppingCart");

// Reassign items requiring drop-shipping to new or existing drop-ship groups
if (shoppingCart) {
	shoppingCart.createDropShipGroups(dispatcher);
}
