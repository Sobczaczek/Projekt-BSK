# The One Simulator - Zadanie Projektowe
## Opis
Projekt dotyczy optymalizacji operacji przesyłania wiadomości między dwoma punktami (Alicja/Bob) z wykorzystaniem protokołu AODV (ang. Ad hoc On-Demand Distance Vector Routing).

## Implementacja 
1. Klasa Routera
    - obsługa zdarzeń i przekazywanie wiadomości,
    - dołączanie do zdarzeń: przekazanie wiadomości, otrzymanie wiadomości, itp,
    - przeciwdziałanie zapętleniom - śledzenie już odwiedzonych routerów w pakietach,
2. Protokół AODV
    - reagowanie na potrzebę wysłania wiadomości,
    -   