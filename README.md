# The One Simulator - Zadanie Projektowe
## Wprowadzenie
Projekt dotyczy optymalizacji operacji przesyłania wiadomości między dwoma punktami (Alicja/Bob) z wykorzystaniem protokołu AODV (ang. Ad hoc On-Demand Distance Vector Routing).

## Implementacja 
1. Klasa Routera
    - obsługa zdarzeń i przekazywanie wiadomości,
    - dołączanie do zdarzeń: przekazanie wiadomości, otrzymanie wiadomości, itp,
    - przeciwdziałanie zapętleniom - śledzenie już odwiedzonych routerów w pakietach.
2. Protokół AODV
    - reagowanie na potrzebę wysłania wiadomości,
    - proaktywne podejście do propagowania informacji o ścieżkach ale z ograniczonym obciążeniem sieci,
    - reaktywne podejście w sytuacji potrzeby natychmiastowego wysłania wiadomości.  
3. Optymalizacja czasu przesyłania
    - pasywne i aktywne podejście do tworzenia ścieżek w zależności od sytuacji,
        - pasywne: router uczy się dynamicznie na podstawie otrzymanych danych,
        - aktywne: przesyłanie zapytania o ścieżki tylko gdy jest to niezbędne.
4. Zapobieganie zapętleniom
    - tworzenie i utrzymywanie tablic routingu - śledzenie odwiedzonych routerów,
    - mechanizmy pakietowe - np. pole `HOP COUNT`,
    - algorytmy usuwające pakiety, które przekroczą ilość skoków.

## Opis 
### Proaktywne vs. Reaktywne
`Proaktywne` podejście polega na ciągłym informowaniu routerów o dostępnych ścieżkach, nawet jeśli nie są używane w danym momencie.

`Reaktywne` podejście aktywuje się tylko wtedy, gdy jest potrzeba przesyłania danych, co zmniejsza obciążenie sieci.

### Zapytania o ścieżki
`Route Request Packet (RREQ)`
https://www.sciencedirect.com/topics/computer-science/route-request-packet

Wykorzystanie specjalnych ramkek z `bitfieldem` do przesyłania zapytań o ścieżki.

Umożliwienie odpowiedzi tylko tym routerom, które widziały zapytanie w określonym czasie.

### Odpowiedzi na zapytania
`Route Reply (RREP)`
https://www.researchgate.net/figure/Propagation-of-Route-Request-RREQ-packet-Route-Reply-RREP-packet_fig1_215994029

Kiedy router dostanie zapytanie o ścieżkę, może odpowiedzieć pakietem zawierającym informacje o trasie.

Informacje zawarte w pakiecie obejmują źródło, adresy pośrednich routerów, HOP COUNT itp.

## Nasz algorytm
### Opis
Zastosowanie protokołu reaktywnego wzorując się na AODV.

### Algorytm
Zakładamy dwa podstawowe scenariusze protokołu komunikacyjnego do przesłania wiadomości z punktu A do punktu B:

a. `Source Node (nadawca)` nie posiada informacji o trasie prowadzącej do odbiorcy:
- `broadcast` zmodyfikowanego pakietu `RREQ` do wszyskich sąsiadów source node'a,
- pakiet `RREQ` będzie zawierał dodatkowe informacje o aktualnym czasie jaki upłynął od utworzenia i wysłania tego pakietu i aktualizowany do czasu dotarcia do odbiorcy `Destination Node`,
- Odbiorca zgromadzi wiadomości i będzię oczekiwał przez jakiś czas na pakiety `RREQ` idące innymi ścieżkami, nadpisując tymi z najbardziej optymalnym czasem dotarcia pakietu od jego źródła,
- znając już trasę potencjalnie najszybszą odsyła nią pakiet `RRES` do pierwotnego nadawcy.

#### Przykładowe struktury pakietów
- `RREQ`:
    - adres odbiorcy,
    - adres nadawcy,
    - numer sekwencji,
    - liczbę przeskoków,
    - czas życia pakietu.
- `RRES`:
    - adres nadawcy,
    - adres odbiorcy,
    - numer sekwencji,
    - liczbę przeskoków.

b. Informacja o trasie jest znana: