package sample.persistence;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * This interface defines all the commands that the ShoppingCart persistent actor supports.
 */
public interface Command extends CborSerializable {
}

/**
 * A command to add an item to the cart.
 * <p>
 * It can reply with `Confirmation`, which is sent back to the caller when
 * all the events emitted by this command are successfully persisted.
 */
class AddItem implements Command {
    public final String itemId;
    public final int quantity;

    public AddItem(String itemId, int quantity) {
        this.itemId = itemId;
        this.quantity = quantity;
    }
}

/**
 * A command to remove an item from the cart.
 */
class RemoveItem implements Command {
    public final String itemId;

    @JsonCreator
    public RemoveItem(String itemId) {
        this.itemId = itemId;
    }
}

class View implements Command {

}
