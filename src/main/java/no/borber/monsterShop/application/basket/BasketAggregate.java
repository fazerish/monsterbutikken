package no.borber.monsterShop.application.basket;

import no.borber.monsterShop.application.Aggregate;
import no.borber.monsterShop.application.CommandValidationException;
import no.borber.monsterShop.application.basket.events.BasketCheckedOut;
import no.borber.monsterShop.application.basket.events.BasketCreated;
import no.borber.monsterShop.application.basket.events.ItemAddedToBasket;
import no.borber.monsterShop.application.basket.events.ItemRemovedFromBasket;
import no.borber.monsterShop.eventStore.Event;

import java.util.List;


class BasketAggregate extends Aggregate {
    private BasketState basketState;

    public BasketAggregate(List<Event> events) {
        for (Event event : events) {
            if (event instanceof BasketCreated)
                updateState(event);
            else if( event instanceof BasketCheckedOut){
                updateState((BasketCheckedOut) event);
            } else if( event instanceof ItemAddedToBasket ) {
                updateState((ItemAddedToBasket) event);
            } else if( event instanceof ItemRemovedFromBasket ) {
                updateState((ItemRemovedFromBasket) event);
            }
            //TODO: Should handle other basket events as well
        }
    }

    public void createBasket(String id) throws CommandValidationException {
        if (basketState != null) {
            throw new CommandValidationException("Attempt to create basket with pre-existing id, create basket failed");
        } else {
            BasketCreated basketCreated = new BasketCreated(id);
            updateState(basketCreated);
            derivedEvents.add(basketCreated);
        }
    }

    public void addItemToBasket(String monsterType) {
        if (basketState == null) {
            throw new CommandValidationException("Attempt to add item from non-existing basket, add item failed");
        } else if(basketState.getBasketCheckedOut()) {
            throw new CommandValidationException("Attempt to add item to basket that has been previously checked out, add item failed");
        } else {
            derivedEvents.add(new ItemAddedToBasket(basketState.getBasketId(), monsterType));
        }
    }

    public void removeItemFromBasket(String monsterType) {
        if (basketState == null) {
            throw new CommandValidationException("Attempt to remove item from non-existing basket, remove item failed");
        } else if (basketState.getBasketCheckedOut()){
            throw new CommandValidationException("Attempt to remove item from basket that has been previously checked out, remove item failed");
        } else {
            derivedEvents.add(new ItemRemovedFromBasket(basketState.getBasketId(), monsterType));
        }
    }

    public List<BasketLineItem> checkoutBasket(){
        if (basketState == null) {
            throw new CommandValidationException("Attempt to checkout non-existing basket, checkout failed");
        } else if (basketState.getBasketLineItems().isEmpty()){
            throw new CommandValidationException("Attempt to checkout empty basket, checkout failed");
        } else if (basketState.getBasketCheckedOut()){
            throw new CommandValidationException("Attempt to checkout basket that has been previously checked out, checkout failed");
        } else {
            basketState.setBasketCheckedOut();
            derivedEvents.add(new BasketCheckedOut(basketState.getBasketId()));
            return basketState.getBasketLineItems();
        }
    }

    private void updateState(BasketCheckedOut event) {
        basketState.setBasketCheckedOut();
    }

    private void updateState(ItemAddedToBasket event) {
        basketState.addMonsterToBasket(event.getMonsterType());
    }

    private void updateState(ItemRemovedFromBasket event) {
        basketState.removeMonsterFromBasket();
    }

    private void updateState(Event event) {
        basketState = new BasketState(event.getAggregateId());
    }

}
