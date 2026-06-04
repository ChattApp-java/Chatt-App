# Chatt-App

Application de messagerie en JavaFX avec client, serveur TCP, gestion des groupes, appels audio/video et persistance MySQL.

## Apercu

Le projet contient :

- un client JavaFX
- un serveur TCP
- un schema SQL pour la base de donnees
- des fonctionnalites de conversations privees et de groupe
- des appels audio/video et reunions

## Technologies

- Java 21
- JavaFX 21
- Maven
- MySQL Connector/J
- HikariCP
- BCrypt
- JavaCV / OpenCV
- SLF4J / Logback

## Structure

- `src/main/java/org/example/tpchatjavafx/client` : interface et logique client
- `src/main/java/org/example/tpchatjavafx/server` : logique serveur
- `src/main/java/org/example/tpchatjavafx/dao` : acces base de donnees
- `src/main/java/org/example/tpchatjavafx/model` : modeles metier
- `src/main/resources/fxml` : vues JavaFX
- `src/main/resources/css` : styles
- `src/main/resources/sql/schema.sql` : schema SQL
- `chattapp_db.sql` : script SQL principal

## Prerequis

- Java 21 installe
- Maven installe
- Une base MySQL ou TiDB accessible

## Base de donnees

Le projet utilise une base nommee `wechat`.

Pour creer les tables, tu peux utiliser :

- `chattapp_db.sql`
- ou `src/main/resources/sql/schema.sql`

La configuration de connexion se trouve dans :

- `src/main/java/org/example/tpchatjavafx/dao/DatabaseConnection.java`

## Compilation

```bash
mvn -DskipTests compile
```

## Lancement

Lancer le client JavaFX :

```bash
mvn javafx:run
```

Lancer le serveur :

```bash
mvn exec:java
```

Lancer serveur + client avec le lanceur principal :

```bash
mvn -Dexec.mainClass=org.example.tpchatjavafx.AppLauncher exec:java
```

## Option 1 Points d'entree

- `org.example.tpchatjavafx.client.MainLauncher` : client
- `org.example.tpchatjavafx.server.ServerLauncher` : serveur
- `org.example.tpchatjavafx.AppLauncher` : serveur + client
- 
##Option 2 – Avec les scripts batch (JAR pré-construit)
Ouvrez un terminal PowerShell (ou cmd) et exécutez :
cd out\artifacts\TPchatJavaFX_jar
.\LANCER.bat                 # Lance le serveur + un client
.\LANCER_CLIENT_SEUL.bat     # Lance uniquement un client supplémentaire (le serveur doit déjà tourner)
## Notes

- Le projet contient deja des dossiers generes comme `target/` et `out/`.
- Le certificat `isrgrootx1.pem` peut etre utile pour une connexion SSL selon la configuration de la base.
