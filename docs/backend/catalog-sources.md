# Источники и политика данных каталога

Дата повторной проверки официальных страниц: 13.07.2026.

| Группа | Официальный источник | Подтверждённые сведения |
|---|---|---|
| Alfa Laval Fast Track | <https://shop.alfalaval.com/en-us/gasketed-plate-heat-exchangers--2244334> | площадь, число и материал пластин, часть габаритов и массы конкретных SKU |
| Danfoss B3 | <https://designcenter.danfoss.com/products/climate-solutions-for-cooling/heat-exchangers/brazed-plate-heat-exchangers/fishbone-brazed-plate-heat-exchangers/bphe-b3/p/111B6315> | B3-012: 40 пластин, -196…200 °C, 30 bar, масса 2,36 кг |
| Kelvion NX | <https://www.kelvion.com/products/plate-heat-exchangers/gasketed-plate-heat-exchangers/> | назначение NX и давление серии до 31 bar |
| SWEP All-Stainless | <https://www.swepgroup.com/challenge-efficiency/technology/swep-all-stainless> | применение, полностью нержавеющая технология, предел технологии до 350 °C; отдельные исполнения до 43 bar |
| Basco | <https://www.apiheattransfer.com/product/type-500/> | материалы, диаметры/длины серии, число ходов; для Type 500 стандартно до 300 psi и 300 °F |
| Kelvion air cooled | <https://www.kelvion.com/products/heat-rejection-heat-recovery-solutions/flatbed-condensers-/-drycooler> | диапазоны мощности LF, LF-S, RF-S и GF-S |
| Kelvion V-shape | <https://www.kelvion.com/products/heat-rejection-heat-recovery-solutions/v-shape-condensers-/-drycooler> | LV-M 6–395 кВт, RV-T 13,2–2150 кВт |
| Alfa Laval SHE LTL | <https://assets.alfalaval.com/documents/pc168354a/alfa-laval-product-leaflet-she-ltl-en.pdf> | площадь, габарит, масса и MAWP при 100/200/300/400 °C |
| Alfa Laval SHE Cond | <https://assets.alfalaval.com/documents/p36beedba/alfa-laval-product-leaflet-she-cond-en.pdf> | площадь, габарит, масса и MAWP при 150/250/300/400 °C |

## Правило заполнения

1. Существующие значения из официальной карточки не заменяются.
2. Из официальной серии переносятся только явно опубликованные общие пределы.
3. Поле, которого нет в открытом источнике, получает детерминированное представительное значение по семейству.
4. Такие поля перечисляются в `mockFields`, показываются в UI с меткой `DEMO` и не используются для инженерного sizing.
5. Полная трассировка записана в [`catalog-data-quality.csv`](catalog-data-quality.csv).
