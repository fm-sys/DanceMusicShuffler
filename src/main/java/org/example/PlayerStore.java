package org.example;

import java.util.List;

public class PlayerStore extends AbstractStore<PlayerState> {
    @Override
    protected PlayerState defaultState() {
        return new PlayerState(null, null, null, false, 0, 0, List.of());
    }
}