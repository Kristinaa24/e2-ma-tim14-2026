package com.tim14.slagalica.repository;

import com.tim14.slagalica.model.AsocijacijeRound;
import com.tim14.slagalica.model.KorakPoKorakRound;
import com.tim14.slagalica.model.KoZnaZnaQuestion;
import com.tim14.slagalica.model.SpojniceRound;
import com.tim14.slagalica.service.SkockoService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class LocalGameRepository {

    private final Random random = new Random();

    private final List<KoZnaZnaQuestion> koZnaZnaQuestions = Arrays.asList(
            new KoZnaZnaQuestion(
                    "q1",
                    "Which river flows through Belgrade?",
                    Arrays.asList("Danube", "Tisa", "Morava", "Drina"),
                    0
            ),
            new KoZnaZnaQuestion(
                    "q2",
                    "Who invented alternating current systems?",
                    Arrays.asList("Thomas Edison", "Nikola Tesla", "Albert Einstein", "Isaac Newton"),
                    1
            ),
            new KoZnaZnaQuestion(
                    "q3",
                    "What is the capital of France?",
                    Arrays.asList("Rome", "Madrid", "Paris", "Berlin"),
                    2
            ),
            new KoZnaZnaQuestion(
                    "q4",
                    "Which planet is known as the Red Planet?",
                    Arrays.asList("Mars", "Venus", "Jupiter", "Mercury"),
                    0
            ),
            new KoZnaZnaQuestion(
                    "q5",
                    "Which sea borders Greece?",
                    Arrays.asList("Baltic Sea", "North Sea", "Aegean Sea", "Black Sea"),
                    2
            ),
            new KoZnaZnaQuestion(
                    "q6",
                    "How many continents are there?",
                    Arrays.asList("Five", "Six", "Seven", "Eight"),
                    2
            )
    );

    private final List<SpojniceRound> spojniceRounds = Arrays.asList(
            new SpojniceRound(
                    "sp1",
                    "Countries and capitals",
                    Arrays.asList("Serbia", "France", "Italy", "Spain", "Germany"),
                    Arrays.asList("Belgrade", "Paris", "Rome", "Madrid", "Berlin"),
                    Arrays.asList("Rome", "Berlin", "Belgrade", "Madrid", "Paris")
            ),
            new SpojniceRound(
                    "sp2",
                    "Authors and works",
                    Arrays.asList("Njegos", "Ivo Andric", "Shakespeare", "Tolstoy", "Servantes"),
                    Arrays.asList("Gorski vijenac", "The Bridge on the Drina", "Hamlet", "War and Peace", "Don Quixote"),
                    Arrays.asList("Hamlet", "Don Quixote", "War and Peace", "Gorski vijenac", "The Bridge on the Drina")
            )
    );

    private final List<AsocijacijeRound> asocijacijeRounds = Arrays.asList(
            new AsocijacijeRound("VODA",
                    "PICE", Arrays.asList("Casa", "Bokal", "Zed", "Sok"),
                    "REKA", Arrays.asList("Dunav", "Sava", "Most", "Obala"),
                    "MORE", Arrays.asList("So", "Plaza", "Plivanje", "Talas"),
                    "KISA", Arrays.asList("Oblak", "Kapi", "Barica", "Grmljavina")
            ),
            new AsocijacijeRound("SKOLA",
                    "DJAK", Arrays.asList("Torba", "Ucenik", "Kecelja", "Klupa"),
                    "UCITELJ", Arrays.asList("Dnevnik", "Ocena", "Tabla", "Kreda"),
                    "KNJIGA", Arrays.asList("Strana", "Citanje", "Slova", "Pisac"),
                    "UCIONICA", Arrays.asList("Vrata", "Prozor", "Stolica", "Odmor")
            )
    );

    public List<AsocijacijeRound> getAsocijacijeRounds() {
        List<AsocijacijeRound> shuffled = new ArrayList<>(asocijacijeRounds);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(2, shuffled.size()));
    }

    public List<KoZnaZnaQuestion> getKoZnaZnaMatchQuestions() {
        List<KoZnaZnaQuestion> shuffled = new ArrayList<>(koZnaZnaQuestions);
        Collections.shuffle(shuffled, random);
        return shuffled.subList(0, Math.min(5, shuffled.size()));
    }

    public List<SpojniceRound> getSpojniceMatchRounds() {
        List<SpojniceRound> shuffled = new ArrayList<>(spojniceRounds);
        Collections.shuffle(shuffled, random);
        return shuffled.subList(0, Math.min(2, shuffled.size()));
    }

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

    public List<String> generateSkockoSecretCombination() {
        List<String> combination = new ArrayList<>();
        SkockoService.Symbol[] symbols = SkockoService.Symbol.values();

        for (int index = 0; index < 4; index++) {
            combination.add(symbols[random.nextInt(symbols.length)].name());
        }

        return combination;
    }
}
