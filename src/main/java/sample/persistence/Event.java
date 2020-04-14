package sample.persistence;


public interface Event extends CborSerializable {
}

final class ItemAdded implements Event {
    public final String cartId;
    public final String itemId;
    public final int quantity;

    public ItemAdded(String cartId, String itemId, int quantity) {
        this.cartId = cartId;
        this.itemId = itemId;
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "ItemAdded(" + cartId + "," + itemId + "," + quantity + ")";
    }
}

final class ItemRemoved implements Event {
    public final String cartId;
    public final String itemId;

    public ItemRemoved(String cartId, String itemId) {
        this.cartId = cartId;
        this.itemId = itemId;
    }

    @Override
    public String toString() {
        return "ItemRemoved(" + cartId + "," + itemId + ")";
    }
}


