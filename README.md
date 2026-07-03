# Slagalica

Mobilna Android aplikacija radjena u okviru projekta iz predmeta Mobilne aplikacije.

Aplikacija implementira igru Slagalica sa korisnickim nalozima, tokenima, zvezdama, ligama, rang listama, regionima Srbije, prijateljima, regionalnim chat-om, izazovima i multiplayer partijama.

## Pokretanje aplikacije

1. Klonirati repozitorijum.
2. Otvoriti projekat u Android Studio-u.
3. Sacekati Gradle sync.
4. Pokrenuti aplikaciju na emulatoru ili fizickom Android uredjaju klikom na Run.
5. Za funkcionalnosti koje koriste Firebase potrebno je da uredjaj ima internet konekciju.


## Testiranje multiplayer igre na dva uredjaja

Za testiranje multiplayer rezima potrebno je koristiti dva uredjaja, dva emulatora ili kombinaciju emulatora i fizickog uredjaja.

### Regularna online partija

1. Ulogovati se na dva razlicita naloga na dva uredjaja.
2. Na oba uredjaja otvoriti Home ekran.
3. Na oba uredjaja kliknuti Start Match.
4. Aplikacija uparuje igrace u zajednicki mec.
5. Prvi igrac igra kao Player 1, drugi kao Player 2.
6. Nakon zavrsene partije azuriraju se zvezde, liga, rang lista i statistika.

### Prijateljska partija

1. Ulogovati se na dva razlicita naloga.
2. Korisnici treba da budu prijatelji.
3. Prvi korisnik u Friends sekciji bira prijatelja i salje invite.
4. Drugi korisnik otvara obavestenje i prihvata invite.
5. Ako drugi korisnik ne prihvati na vreme, poziv istice.
6. Nakon prihvatanja oba korisnika ulaze u prijateljsku partiju.


