package sample.persistence;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.persistence.typed.PersistenceId;
import akka.persistence.typed.javadsl.CommandHandler;
import akka.persistence.typed.javadsl.CommandHandlerBuilder;
import akka.persistence.typed.javadsl.Effect;
import akka.persistence.typed.javadsl.EventHandler;
import akka.persistence.typed.javadsl.EventSourcedBehavior;
import akka.persistence.typed.javadsl.RetentionCriteria;

import java.util.HashMap;
import java.util.Map;

/**
 * This is an event sourced actor. It has a state, {@link ShoppingCart.State}, which
 * stores the current shopping cart items and whether it's checked out.
 * <p>
 * Event sourced actors are interacted with by sending them commands,
 * see classes implementing {@link Command}.
 * <p>
 * Commands get translated to events, see classes implementing {@link Event}.
 * It's the events that get persisted by the entity. Each event will have an event handler
 * registered for it, and an event handler updates the current state based on the event.
 * This will be done when the event is first created, and it will also be done when the entity is
 * loaded from the database - each event will be replayed to recreate the state
 * of the entity.
 */
public class ShoppingCart
        extends EventSourcedBehavior<Command, Event, ShoppingCart.State> {

    private final String cartId;
    private final ActorContext actorContext;
    private final ShoppingCartCommandHandlers shoppingCartCommandHandlers = new ShoppingCartCommandHandlers();

    public ShoppingCart(ActorContext context, String cartId) {
        super(PersistenceId.of("ShoppingCart", cartId));
        this.cartId = cartId;
        this.actorContext = context;
    }

    public static Behavior<Command> create(String cartId) {
        return Behaviors.setup(context -> new ShoppingCart(context, cartId));
    }

    @Override
    public State emptyState() {
        return new State();
    }

    @Override
    public CommandHandler<Command, Event, State> commandHandler() {
        CommandHandlerBuilder<Command, Event, State> b =
                newCommandHandlerBuilder();

        b.forAnyState()
                .onCommand(AddItem.class, shoppingCartCommandHandlers::onAddItem)
                .onCommand(RemoveItem.class, shoppingCartCommandHandlers::onRemoveItem)
                .onCommand(View.class, this::print);

        return b.build();
    }

    private Effect<Event, State> print(State state, View viewCommand) {
        if (state.isEmpty()) {
            System.out.println("No items available");
        } else {
            System.out.println("Inventory Details:" + state.items);
        }
        return Effect().none();
    }

    @Override
    public EventHandler<State, Event> eventHandler() {
        return newEventHandlerBuilder().forAnyState()
                .onEvent(ItemAdded.class, (state, event) -> state.updateItem(event.itemId, event.quantity))
                .onEvent(ItemRemoved.class, (state, event) -> state.removeItem(event.itemId))
                .build();
    }

    @Override
    public RetentionCriteria retentionCriteria() {
        // enable snapshot
        return RetentionCriteria.snapshotEvery(4, 3);
    }

    /**
     * The state for the {@link ShoppingCart} entity.
     */
    public static final class State implements CborSerializable {
        private Map<String, Integer> items = new HashMap<>();

        public boolean isEmpty() {
            return items.isEmpty();
        }

        public boolean hasItem(String itemId) {
            return items.containsKey(itemId);
        }

        public State updateItem(String itemId, int quantity) {
            items.put(itemId, quantity);
            return this;
        }

        public State removeItem(String itemId) {
            items.remove(itemId);
            return this;
        }
    }

    private class ShoppingCartCommandHandlers {

        Effect<Event, State> onAddItem(State state, AddItem cmd) {
            if (state.hasItem(cmd.itemId)) {
                System.out.println("Item '" + cmd.itemId + "' was already added to this shopping cart");
                return Effect().none();
            } else {
                return Effect().persist(new ItemAdded(cartId, cmd.itemId, cmd.quantity));
            }
        }

        Effect<Event, State> onRemoveItem(State state, RemoveItem cmd) {
            if (state.hasItem(cmd.itemId)) {
                return Effect().persist(new ItemRemoved(cartId, cmd.itemId))
                        .thenRun(updatedCart -> System.out.println("Updated cart " + updatedCart.items + " after removing an item:" + cmd.itemId));
            } else {
                System.out.println("Cart does'nt have given item:" + cmd.itemId);
            }
        }
    }

    public static void main(String[] args) {
        String cartId = "Demo13";
        ActorSystem<Command> actorSystem = ActorSystem.create(ShoppingCart.create(cartId), "AkkaDemo");
        actorSystem.tell(new View());
        actorSystem.tell(new AddItem("foo", 42));
        actorSystem.tell(new View());
        actorSystem.tell(new RemoveItem("bar"));
        actorSystem.tell(new View());
        actorSystem.tell(new AddItem("bar", 25));
        actorSystem.tell(new View());
        actorSystem.tell(new RemoveItem("foo"));
        actorSystem.tell(new View());
    }
}


