package com.tim14.slagalica.repository;

import com.tim14.slagalica.model.KorakPoKorakRound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class LocalGameRepository {

    private final Random random = new Random();

    private final List<KorakPoKorakRound> korakRounds = Arrays.asList(
            new KorakPoKorakRound(
                    "Sunce",
                    "1. Nebesko telo",
                    "2. Daje svetlost",
                    "3. Daje toplotu",
                    "4. Izlazi ujutru",
                    "5. Zalazi uvece",
                    "6. Zute je boje",
                    "7. Centar Suncevog sistema"
            ),
            new KorakPoKorakRound(
                    "Tesla",
                    "1. Poznati naucnik",
                    "2. Rodjen na Balkanu",
                    "3. Bavio se strujom",
                    "4. Radio u Americi",
                    "5. Ima jedinicu u fizici po sebi",
                    "6. Zvao se Nikola",
                    "7. Prezime mu je povezano sa tehnologijom"
            ),
            new KorakPoKorakRound(
                    "Dunav",
                    "1. Reka",
                    "2. Prolazi kroz Evropu",
                    "3. Protice kroz Srbiju",
                    "4. Uliva se u Crno more",
                    "5. Ima vise pritoka",
                    "6. Jedna je od najduzih evropskih reka",
                    "7. Prolazi kroz Beograd"
            )
    );

    public KorakPoKorakRound getRandomKorakPoKorakRound() {
        return korakRounds.get(random.nextInt(korakRounds.size()));
    }

    public List<KorakPoKorakRound> getKorakPoKorakMatchRounds() {
        List<KorakPoKorakRound> shuffledRounds = new ArrayList<>(korakRounds);
        Collections.shuffle(shuffledRounds, random);
        return shuffledRounds.subList(0, Math.min(2, shuffledRounds.size()));
    }

    public int generateTargetNumber() {
        return random.nextInt(900) + 100;
    }

    public int[] generateOfferedNumbers() {
        int[] middleNumbers = {10, 15, 20};
        int[] bigNumbers = {25, 50, 75, 100};

        return new int[]{
                random.nextInt(9) + 1,
                random.nextInt(9) + 1,
                random.nextInt(9) + 1,
                random.nextInt(9) + 1,
                middleNumbers[random.nextInt(middleNumbers.length)],
                bigNumbers[random.nextInt(bigNumbers.length)]
        };
    }
}
