# The One Simulator - Zadanie Projektowe
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