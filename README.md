insee-200m-extract
==================

Présentation
------------

Extracteur de données INSEE carroyées 200m au format GeoTiff / CSV

Ce programme extrait une partie des données carroyées 200m
publiées par l'INSEE, au format GeoTiff et/ou CSV.

Ce programme est un logiciel libre (licence EUPL v.1.1)
développé par Mecatran pour le compte du DREAL PACA.
Pour plus d'information sur la licence,
[veuillez consulter cette page](https://joinup.ec.europa.eu/software/page/eupl/licence-eupl).

Les données des rectangles sont fusionnées aux carrés selon
la méthode préconisée par l'INSEE (voir méthodologie en ligne).

Deux formats sont disponibles: GeoTIFF ou CSV.
En GeoTiff, le CRS est celui des données sources (EPSG:3035).
En CSV, il est possible de choisir le CRS de sortie.

Les variables disponibles sont celles des données publiées (population, ages, revenus, mégages...)
Elles sont disponibles en sortie à la fois sous forme de somme
sur un carreau (suffixe 'sum') et normalisé par individu ou ménage (suffixe 'norm').

Pour plus d'information sur les variables disponibles,
[veuillez consulter cette page](http://www.insee.fr/fr/themes/detail.asp?reg_id=0&ref_id=donnees-carroyees).

Utilisation
-----------

1. Installer un JRE java.
2. Télécharger le programme (format zip). Extraire le zip dans un répertoire.
3. Télécharger les données carroyées 200m de l'INSEE (table des carreaux et des rectangles).
4. Extraire des zip des données INSEE les fichiers `car_m.dbf` et `rect_m.dbf`, les placer ensemble dans un répertoire.
5. Ouvrir une ligne de commande et se placer dans le répertoire __où se trouvent les deux fichiers `*.dbf`__.
6. Lancer le programme, par exemple: `../insee-200m-extract-0.1/bin/insee-200m-extract --left 5.23 --bottom 43.16 --right 5.58 --top 43.40 --csv --geotiff --outputPrefix INSEE_200m_Marseille` pour extraire les données autour du centre de Marseille.
