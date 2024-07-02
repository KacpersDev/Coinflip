package net.pulsir.multicoinflip.coinflip;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CoinFlip {

    private String authorName, playerName;
    private UUID author;
    private UUID player;
    private double amount;

    public CoinFlip(String authorName, String playerName, UUID author, UUID player, double amount) {
        this.authorName = authorName;
        this.playerName = playerName;
        this.author = author;
        this.player = player;
        this.amount = amount;
    }
}
