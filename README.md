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



Testiranje igre na dva uređaja
Za testiranje multiplayer režima potrebno je koristiti dva uređaja (ili dva emulatora).

Koraci
Ulogovati se na dva različita naloga na dva uređaja.
Na prvom uređaju izabrati:
Start Match
Create 2-device match
Zapisati prikazani Room Code i kliknuti na Open Room.
Na drugom uređaju izabrati:
Start Match
Join 2-device match
Uneti isti Room Code koji je prikazan na prvom uređaju.
Nakon uspešnog povezivanja:
Prvi uređaj igra kao Player 1
Drugi uređaj igra kao Player 2
Igra može da počne nakon povezivanja oba igrača.
Napomene
Oba uređaja moraju imati internet konekciju.
Za testiranje se mogu koristiti dva fizička uređaja, dva emulatora ili kombinacija emulatora i fizičkog uređaja.
Pre pokretanja multiplayer meča potrebno je da oba korisnika budu uspešno prijavljena u aplikaciju.

