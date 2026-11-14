# Avto Budilka (Wear OS)

Ko ura zazna, da si **zaspal**, sama nastavi alarm cez **7 h 30 min**.
Narejeno za Samsung Galaxy Watch 8, deluje na vseh urah z Wear OS 3+.

- Vklop / izklop v aplikaciji
- Rezervni alarm: brez / +5 / +10 min (ce prespis prvega)
- Uposteva samo **prvo** zaspanje v noci (12 h okno), kasnejsa prebujanja ignorira
- Zvok + vibracija ali samo vibracija
- Ko se alarm nastavi, dobis tiho obvestilo z gumbom **Preklici** (ce si samo zadremal)
- Varcno z baterijo: app ne tece v ozadju, ura jo poklice samo ob spremembi stanja (Health Services)

## Namestitev
1. Odpri projekt v Android Studiu in pocakaj, da se Gradle sinhronizira.
2. Na uri vklopi *Developer options* → *ADB debugging* + *Debug over Wi-Fi*.
3. `adb connect <IP-ure>:<port>` in pritisni **Run**.
4. V aplikaciji vklopi stikalo in dovoli "Telesna aktivnost".
