# Peterwolf's Groundworks Asphalt

Submod do ekosystemu **Peterwolf's Groundworks** wprowadzający realistyczne maszyny do budowy dróg asfaltowych, profilowania podłoża i zagęszczania nawierzchni dla **Minecraft 26.3 (Fabric)**.

---

## 🚜 Maszyny Drogowe

### 1. Cat Motor Grader (Równiarka Drogowa)
- 6-kołowy układ jezdny (przednia oś skrętna z pochyleniem kół *wheel lean*, tylny tandem napędowy 4 kół).
- Długa łukowata rama nośna (gooseneck / arched backbone).
- Środkowy wieniec obrotowy z parabolicznym lemieszem profilującym (*moldboard*) o regulowanej wysokości, kącie i przesunięciu poprzecznym.
- Funkcja: skrawanie i wyrównywanie nierówności gruntu, dirtu, piasku i żwiru do zadanej rzędnej roboczej.

### 2. Asphalt Paver (Rozściełacz Asfaltu)
- Ciężkie podwozie gąsienicowe.
- Przedni kosz zasypowy z hydraulicznymi składanymi skrzydłami (pojemność na masę bitumiczną dowożoną z wywrotek Groundworks lub ładowarek).
- Centralny tunel ze ślimakami rozprowadzającymi masę.
- Zadaszony pomost operatora z dwoma obrotowymi fotelami.
- Tylny pływający stół zagęszczający (screed) o szerokości 5 bloków, układający pas gorącego, parującego świeżego asfaltu po krzywiźnie terenu.

### 3. Road Roller 16T (Walec Drogowy)
- Przedni ciężki stalowy bęben wibracyjny w potężnym jarzmie ze zgarniaczami i zraszaczami.
- Centralny przegub skrętny (skręt do 35°).
- Nowoczesna panoramiczna kabina ROPS/FOPS z klimatyzacją i oświetleniem roboczym LED.
- Tylna oś z kołami pneumatycznymi o głębokim bieżniku traktorowym.
- Funkcja: dynamiczne wibracyjne zagęszczanie świeżego asfaltu (`fresh_asphalt`) w gładki, nośny asfalt drogowy (`compacted_asphalt`) ze zmniejszonym tarciem i bonusem prędkości.

---

## 🧱 Materiały i Bloki Asfaltowe

- **Asphalt Mix (`asphalt_mix`)**: Gorąca mieszanka mineralno-asfaltowa wytwarzana ze żwiru, piasku i węgla/smoły.
- **Fresh Asphalt (`fresh_asphalt`)**: Świeżo rozściełany, gorący asfalt parujący dymem. Lekko spowalnia pieszych (`speedFactor = 0.85F`).
- **Compacted Asphalt (`compacted_asphalt`)**: Solidna, twarda, gładka nawierzchnia drogowa (`friction = 0.55F`) dająca znaczny bonus do prędkości pojazdów i graczy.
- **Asphalt Slab (`asphalt_slab`)**: Półbloki do budowy płynnych ramp i zjazdów.
- **Road Lines (`asphalt_white_line`, `asphalt_yellow_line`)**: Bloki poziomego oznakowania dróg.

---

## 🛠️ Jakość 3D i Deglitch
Wszystkie modele 3D oraz atlasy tekstur 512x512 przeszły rygorystyczny audyt narzędziami `detect_coplanar.py` oraz `check_textures.py`:
- **0 konfliktów koplanarnych (zero Z-fightingu)** dzięki chirurgicznym mikro-offsetom 0.01F.
- **100% pokrycia tekstur** bez prześwitów i dziur.
