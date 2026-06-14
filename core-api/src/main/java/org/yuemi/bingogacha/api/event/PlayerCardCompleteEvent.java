package org.yuemi.bingogacha.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.yuemi.bingogacha.api.model.CardTemplate;
import org.yuemi.bingogacha.api.model.PlayerCard;

public class PlayerCardCompleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PlayerCard playerCard;
    private final CardTemplate cardTemplate;

    public PlayerCardCompleteEvent(
            @NotNull Player player,
            @NotNull PlayerCard playerCard,
            @NotNull CardTemplate cardTemplate
    ) {
        this.player = player;
        this.playerCard = playerCard;
        this.cardTemplate = cardTemplate;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public PlayerCard getPlayerCard() {
        return playerCard;
    }

    @NotNull
    public CardTemplate getCardTemplate() {
        return cardTemplate;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
