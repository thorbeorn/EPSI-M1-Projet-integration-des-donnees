# 🍽️ Projet ETL Openfoodfacts

Ce projet consiste à mettre en place une solution ETL (Extract, Transform, Load) distribuée pour générer aléatoirement des menus alimentaires adaptés aux besoins des utilisateurs, en utilisant les données disponibles sur OpenFoodFacts.

## 📝 Description du Projet

Le client a exprimé le besoin de créer un système capable de générer des menus alimentaires équilibrés sur une semaine, conformément aux régimes alimentaires sélectionnés par les utilisateurs. Les menus doivent respecter les seuils nutritionnels spécifiques à chaque régime, tout en tenant compte des critères tels que les sucres, les sucres-ajoutés, etc.

## 🎯 Objectifs

1. Collecter les données depuis OpenFoodFacts. 
2. Créer des sources de données supplémentaires pour les régimes alimentaires et les utilisateurs. 
3. Assurer la qualité des données en appliquant des critères de sélection. 
4. Transformer les données pour générer des menus alimentaires équilibrés en fonction des régimes alimentaires des utilisateurs. 
5. Charger les menus générés dans un Data Warehouse (DWH).

## 🛠️ Spécificités Techniques

- **Langage de programmation :** Java
- **Outils ETL :** Apache Spark
- **Source des données :** OpenFoodFacts (https://fr.openfoodfacts.org/data)
- **Bases de données :** MySQL (pour le DWH)

## 📋 Prérequis
Pour pouvoir exécuter ce projet, vous devez avoir installé les logiciels suivants :
- [Docker](https://www.docker.com/)
- [Java 17](https://www.oracle.com/java/technologies/javase/jdk20-archive-downloads.html)

## 🚀 Installation

1. **Cloner le projet :**
```bash
git clone https://github.com/thorbeorn/EPSI-M1-Projet-integration-des-donnees.git
```

2. **Ouvrir le projet dans un IDE (IntelliJ IDEA, Eclipse, etc.) :**
```bash
cd openfoodfacts-etl
```

3. **Télécharger le CSV d'OpenFoodFacts et le placer dans le dossier** `data/` **avec les autres fichiers CSV. Vous pouvez télécharger le fichier CSV depuis ce lien :** [**OpenFoodFacts**](https://static.openfoodfacts.org/data/en.openfoodfacts.org.products.csv)
```bash
mv en.openfoodfacts.org.products.csv data/products.csv
```

4. **Installation des dépendances Maven si ce n'est pas déjà fait :** (*voir le fichier `pom.xml` pour plus de détails.*)
```bash
mvn install
```

5. **Lancez le conteneur MySQL :**
```bash
docker-compose up -d
```

6. **Vérifiez que le conteneur MySQL est en cours d'exécution :**
```bash
docker ps
```

7. **Compiler le projet avant de l'exécuter :**

```bash
mvn clean package
```

8. **Exécutez la classe principale `Main.java` pour lancer le projet :**
```bash
mv etl/target/*with*.jar etl.jar
```

### - Single node
```bash
docker exec -it spark-master /bin/bash
```
```bash
cd /app
sh /spark/bin/spark-submit \
/app/etl.jar
```

### - cluster
```bash
docker exec -it spark-master /bin/bash
```
```bash
cd /app
sh /spark/bin/spark-submit \
--master spark://spark-master:7077 \
/app/etl.jar
```

## 📦 Structure du Projet

```
openfoodfacts-etl
│
├── README.md // Fichier README
├── LICENSE // Licence du projet
├── conf
│   └── docker-compose.yml // Fichier Docker Compose
│
├── data
│   ├── en.openfoodfacts.org.products.csv // Fichier CSV OpenFoodFacts à télécharger
│   ├── diets.csv // Fichier CSV des régimes alimentaires 
│   ├── contries.csv // Fichier CSV des pays avec les traductions 
│   └── users.csv // Fichier CSV des utilisateurs
│
├── etl
│   ├── src
│   │   └── main
│   │       ├── java
│   │       │   └── etl
│   │       │       ├── Constants.java // Constantes du projet
│   │       │       ├── Main.java // Classe principale du projet
│   │       │       ├── Extrator.java // Extraction des données
│   │       │       ├── Transformer.java // Transformation des données
│   │       │       ├── Generator.java // Génération des menus
│   │       │       ├── Utils.java // Méthodes utilitaires
│   │       │       └── Loader.java // Chargement des données
│   │       │
│   │       └── resources
│   │
│   ├── pom.xml // Fichier de configuration Maven     
│   └── targer // Dossier de sortie pour les build java
│
├── sql
│   ├── bdd_for_bulk.md // Document pour initialiser la bdd de bulk de openfoodfact en cas d'utilisation de mongoDB dump
│   ├── init.sql // Script d'initialisation de la base de données
│   └── README.md // Initialisation de l’environnement Docker + Restauration du dump MongoDB + Création des tables SQL
│   
└── docs
    ├── data
    │    ├── cahier de qualité.md
    │    ├── jeu de requete.md
    │    └── show header products.py
    │
    ├── diagram
    │    ├── mld.png // Diagramme de l'architecture du projet
    │    ├── mld.azimutt.json // Fichier Azimutt du diagramme
    │    └── README.md // Instructions pour générer le diagramme
    ├── schemas
    │    ├── schema-openfoodfacts.png // Schéma de du projet
    │    └── workflows-openfoodfacts.png // Workflows du projet
    ├── sujet
    │    └── 251112 Projet TRDE703 Atelier Intégration des Données 2025-2026.pdf // Sujet du projet
    └── images
         ├── Table user.png
         ├── Table regime.png
         └── Table daily_menu.png
```

## 📄 Documentation

Pour plus d'informations sur le projet, veuillez consulter la documentation disponible dans le dossier `docs/`.
Voir ci-dessous les différentes parties de la documentation :

## 📜 License
Ce projet est sous licence MIT - voir le fichier [LICENSE](./LICENSE) pour plus de détails.