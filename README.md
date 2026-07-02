# Slagalica

Mobilna Android aplikacija radjena u okviru projekta iz predmeta Mobilne aplikacije.

Aplikacija implementira igru Slagalica sa korisnickim nalozima, tokenima, zvezdama, ligama, rang listama, regionima Srbije, prijateljima, regionalnim chat-om, izazovima i multiplayer partijama.

## Pokretanje aplikacije

1. Klonirati repozitorijum.
2. Otvoriti projekat u Android Studio-u.
3. Sacekati Gradle sync.
4. Pokrenuti aplikaciju na emulatoru ili fizickom Android uredjaju klikom na Run.
5. Za funkcionalnosti koje koriste Firebase potrebno je da uredjaj ima internet konekciju.

## Nalozi i pristup

Aplikacija podrzava:

- registraciju korisnika,
- prijavu korisnika,
- guest rezim.

Registrovani korisnik ima pristup profilu, statistici, regionima, rang listama, prijateljima, obavestenjima, chat-u, izazovima, tokenima i napredovanju kroz lige.

Guest korisnik moze da pokrene osnovnu partiju, ali nema sacuvan profil, zvezde, tokene, prijatelje, dnevne misije, rang liste ni regionalne funkcionalnosti. Zakljucane opcije ga vode na login/register poruku.

## Glavne funkcionalnosti

### 1. Autentifikacija i profil

- Korisnik se registruje pomocu email-a, lozinke, korisnickog imena i regiona.
- Podaci o korisniku cuvaju se u Firebase Firestore bazi.
- Korisnik moze da vidi profil, avatar, region, broj tokena, broj zvezda, ligu i regionalni okvir avatara.
- Profil sadrzi i statistiku po igrama.

### 2. Igra Slagalica

Partija prolazi kroz igre iz projekta:

- Moj broj,
- Spojnice,
- Ko zna zna,
- Korak po korak,
- Asocijacije,
- Skocko.

Igra se moze igrati kao:

- regularna online partija,
- prijateljska partija,
- turnirska partija,
- challenge solo partija,
- guest partija bez cuvanja napretka.

### 3. Tokeni

- Registrovani korisnik trosi tokene za igranje partija.
- Korisnik dobija osnovne dnevne tokene.
- Liga donosi dodatne dnevne tokene.
- Sto je visa liga, korisnik dobija vise dodatnih tokena.

### 4. Rang liste

Aplikacija ima:

- nedeljnu rang listu igraca,
- mesecnu rang listu igraca,
- mesecnu rang listu regiona.

Rang liste se zasnivaju na zvezdama osvojenim u odgovarajucem ciklusu. Na kraju ciklusa se rezultati resetuju, a nagrade/kazne se primenjuju prema pravilima projekta.

Za potrebe demonstracije postoji test dugme za reset mesecnog ciklusa regiona, jer projekat nema backend scheduler koji bi radio automatski jednom mesecno.

### 5. Prikaz regiona

Ekran regiona prikazuje mapu Srbije pomocu OpenStreetMap prikaza.

Implementirano je:

- prikaz regiona Srbije na mapi,
- nasumicna tacka unutar regiona za igraca koji je izabrao taj region,
- ikonice za regione,
- mesecna rang lista regiona,
- posebno oznacen region trenutnog igraca,
- statistika regiona klikom na region,
- broj prvih, drugih i trecih mesta regiona,
- broj trenutno aktivnih igraca,
- broj ukupno registrovanih igraca,
- dodela zlatnog, srebrnog ili bronzanog okvira avatara igracima iz regiona koji je u prethodnom ciklusu bio u top 3.

### 6. Napredovanje kroz lige

Korisnik napreduje kroz lige na osnovu ukupnog broja zvezda.

Lige:

- No League,
- Bronze League,
- Silver League,
- Gold League,
- Diamond League,
- Master League.

Pravila:

- prva liga pocinje od 100 zvezda,
- svaka naredna liga trazi duplo vise zvezda od prethodne,
- korisnik automatski ulazi u visu ligu kada predje granicu,
- korisnik automatski ispada u nizu ligu kada izgubi dovoljno zvezda,
- liga utice na broj dnevnih tokena,
- ako se korisnik ne plasira na mesecnu rang listu, gubi 30% zvezda,
- promena lige prikazuje dijalog kada je korisnik u aplikaciji i cuva obavestenje u Alerts.

### 7. Prijatelji

Registrovani korisnik moze:

- da vidi listu prijatelja,
- da pretrazuje igrace po korisnickom imenu,
- da doda prijatelja preko username pretrage,
- da doda prijatelja skeniranjem QR koda,
- da vidi sliku, username, mesecni rang, zvezde i ligu prijatelja,
- da posalje zahtev za prijateljsku partiju ako je prijatelj online i nije vec u partiji,
- da otkaze poslati zahtev.

Korisnik koji primi zahtev moze da ga prihvati ili odbije. Ako ne reaguje, zahtev automatski istice.

### 8. Regionalni chat

- Registrovani korisnik ima chat sobu za svoj region.
- Poruke se cuvaju u Firestore bazi.
- Korisnici iz istog regiona vide iste poruke.
- Za nove chat poruke se cuvaju obavestenja.

### 9. Izazovi

Na stranici regiona postoji challenge deo.

Korisnik moze:

- da napravi regionalni izazov,
- da ulozi zvezde i tokene,
- da prihvati tudji izazov,
- da odigra solo challenge partiju,
- da vidi rezultate izazova.

Izazov moze imati najvise 4 ucesnika. Pobednik dobija 75% ukupnog uloga, a sledeci najbolje plasirani dobija nazad svoj ulog.

### 10. Obavestenja

Aplikacija cuva i prikazuje obavestenja za:

- promenu lige,
- mesecne ranking promene,
- regionalni chat,
- prijateljske pozive,
- druge bitne dogadjaje u aplikaciji.

Sistemske notifikacije rade preko Android notification mehanizma kada aplikacija ima dozvolu za notifikacije. Za produkcionu verziju bi se backend deo mogao prosiriti Firebase Cloud Functions/FCM resenjem.

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


