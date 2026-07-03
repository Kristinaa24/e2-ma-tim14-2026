package com.tim14.slagalica.model;

import java.util.ArrayList;
import java.util.List;

public class SharedMojBrojRound {

    public int targetNumber;
    public List<Integer> offeredNumbers;

    public SharedMojBrojRound() {
        offeredNumbers = new ArrayList<>();
    }

    public SharedMojBrojRound(int targetNumber, List<Integer> offeredNumbers) {
        this.targetNumber = targetNumber;
        this.offeredNumbers = offeredNumbers;
    }
}
