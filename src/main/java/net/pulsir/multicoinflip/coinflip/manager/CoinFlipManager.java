package net.pulsir.multicoinflip.coinflip.manager;

import lombok.Getter;
import net.pulsir.multicoinflip.coinflip.CoinFlip;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class CoinFlipManager {

    private final Map<UUID, CoinFlip> coinFlipMap = new HashMap<>();
    private final Map<UUID, UUID> pendingCoinFlips = new HashMap<>();
}
