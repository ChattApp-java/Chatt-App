# RAPPORT DE PROJET

## CHATT-APP

### Application De Messagerie Instantanee En Java

---

## Page De Garde

**Etablissement :** ..........................................................

**Filiere :** ..........................................................

**Module :** Projet / Genie Logiciel / Java

**Theme :** Conception et realisation d'une application de messagerie instantanee

**Titre du projet :** `Chatt-App`

**Realise par :**

- Nom et prenom 1
- Nom et prenom 2
- Nom et prenom 3
- Nom et prenom 4

**Encadre par :** ..........................................................

**Annee universitaire :** 2025 - 2026

---

## Dedicace

Nous dedicacons ce travail a toutes les personnes qui nous ont soutenus durant la realisation de ce projet, en particulier nos enseignants, nos familles et nos camarades, pour leurs encouragements, leurs conseils et leur accompagnement.

---

## Remerciements

Nous exprimons nos sinceres remerciements a notre encadrant pour son suivi, ses orientations et ses remarques constructives. Nous remercions egalement l'ensemble des enseignants qui ont contribue a notre formation et nous ont permis d'acquerir les competences necessaires pour mener a bien ce projet.

Nous remercions aussi nos proches pour leur soutien moral ainsi que tous ceux qui ont participe, de pres ou de loin, a la realisation de `Chatt-App`.

---

## Resume

`Chatt-App` est une application de messagerie instantanee developpee en Java dans une architecture client-serveur. Le projet a pour objectif de proposer une solution de communication integree permettant l'authentification des utilisateurs, la gestion des contacts, l'echange de messages prives et de groupe, le partage de fichiers ainsi que les appels audio, video et les reunions.

L'application repose sur `JavaFX` pour l'interface graphique, `MySQL` pour la persistance des donnees, `Maven` pour la gestion du projet et plusieurs services reseau TCP/UDP pour les communications en temps reel. Ce rapport presente le contexte du projet, son architecture technique, les fonctionnalites implementees, les choix technologiques, l'etat d'avancement ainsi que les perspectives d'amelioration.

**Mots-cles :** messagerie instantanee, JavaFX, MySQL, client-serveur, audio, video, groupe, reunion.

---

## Sommaire

1. Introduction
2. Contexte Et Problematique
3. Objectifs Du Projet
4. Cahier Fonctionnel De L'Application
5. Architecture Generale Du Systeme
6. Presentation Detaillee Des Modules
7. Base De Donnees
8. Technologies Utilisees
9. Fonctionnalites Principales
10. Analyse Technique Du Projet
11. Points Forts
12. Limites Et Difficultes
13. Recommandations D'Amelioration
14. Conclusion

---

## 1. Introduction

Chatt-App est une application de communication instantanée développée en Java. Le projet a pour objectif de proposer une plateforme de messagerie moderne intégrant plusieurs services de communication en temps réel, notamment l'authentification des utilisateurs, l'échange de messages privés, la gestion des contacts, les groupes de discussion, le partage de fichiers ainsi que les appels audio, vidéo et les réunions de groupe.

L'application repose sur une architecture client-serveur. Le client est développé avec JavaFX afin d'offrir une interface graphique desktop moderne et interactive. Le serveur est conçu pour gérer plusieurs connexions simultanées, assurer le routage des messages et coordonner les échanges en temps réel. Une base de données MySQL est utilisée pour la persistance des données, tandis qu'un relais UDP est mobilisé pour la transmission des flux audio et vidéo.

Ce rapport présente l'analyse complète du projet, son architecture, les technologies utilisées, les fonctionnalités disponibles, les points forts, les limites actuelles ainsi que des recommandations d'amélioration.

## 2. Objectifs Du Projet

Les objectifs principaux de Chatt-App sont les suivants :

- permettre à plusieurs utilisateurs de créer un compte et de se connecter de manière sécurisée ;
- offrir un système de messagerie privée en temps réel ;
- conserver l'historique des conversations dans une base de données ;
- gérer les contacts et afficher les statuts de présence ;
- proposer des groupes de discussion ;
- permettre l'envoi de fichiers et de contenus multimédias ;
- intégrer des appels audio et vidéo ;
- supporter des réunions de groupe avec plusieurs participants.

Le projet vise donc à reproduire plusieurs fonctionnalités présentes dans des applications de messagerie modernes, tout en restant dans un cadre académique et pédagogique.

## 3. Présentation Générale De L'Architecture

L'application suit une architecture en couches organisée autour de plusieurs modules :

- une couche cliente responsable de l'interface graphique et des interactions utilisateur ;
- une couche réseau chargée de la communication entre client et serveur ;
- une couche serveur qui gère les connexions, les messages, les groupes et les réunions ;
- une couche d'accès aux données qui dialogue avec MySQL ;
- une couche modèle qui représente les entités métier du système.

Le point d'entrée du client se trouve dans [ChatClientApp.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/client/ChatClientApp.java:14). Cette classe initialise l'application JavaFX, charge l'écran de connexion puis ouvre la vue principale de discussion.

Le point d'entrée du serveur est [ChatServer.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/server/ChatServer.java:20). Le serveur écoute les connexions TCP sur le port `5555`, gère les clients connectés et démarre également un relais UDP pour les communications audio et vidéo.

## 4. Structure Du Projet

Le projet est organisé en plusieurs packages principaux :

- `client` : application cliente, contrôleurs JavaFX, services audio et vidéo ;
- `server` : serveur principal, gestion des sessions, réunions et diffusion UDP ;
- `dao` : classes d'accès à la base de données ;
- `model` : objets métier comme `Utilisateur`, `Message`, `Groupe`, `Reunion` ;
- `common` : éléments partagés entre client et serveur, comme les types de messages ;
- `resources` : fichiers FXML, feuilles de style CSS et schéma SQL.

Cette organisation facilite la maintenance du code et la séparation des responsabilités.

## 5. Technologies Utilisées

Le projet utilise les technologies suivantes :

- `Java 21` comme langage principal ;
- `JavaFX 21` pour l'interface graphique ;
- `Maven` pour la gestion du projet et des dépendances ;
- `MySQL` comme système de gestion de base de données ;
- `HikariCP` pour le pool de connexions ;
- `BCrypt` pour le hashage sécurisé des mots de passe ;
- `webcam-capture` et `JavaCV` pour la capture vidéo ;
- `SLF4J` et `Logback` pour la journalisation ;
- `Ikonli` pour certaines icônes graphiques.

Le fichier [pom.xml](C:/Users/pc/Music/Chatt-App/pom.xml:16) confirme l'utilisation de ces bibliothèques et montre que le projet a été construit avec une base technique moderne.

## 6. Fonctionnement Général Du Système

Le fonctionnement global de l'application peut être résumé de la manière suivante :

1. L'utilisateur lance l'application cliente.
2. L'interface de connexion s'affiche.
3. L'utilisateur peut s'inscrire ou se connecter.
4. Le client communique avec le serveur via TCP.
5. Le serveur vérifie les informations d'identification à partir de la base de données.
6. Une fois connecté, l'utilisateur accède à l'interface principale.
7. Il peut envoyer des messages, consulter ses contacts, ouvrir des conversations, créer ou rejoindre des groupes.
8. Pour les réunions et les médias temps réel, le serveur coordonne aussi l'utilisation d'un relais UDP.

Le protocole d'échange repose sur la classe [ChatMessage.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/client/model/ChatMessage.java:6), qui transporte les informations liées aux messages, appels, groupes et réunions.

## 7. Gestion Des Utilisateurs Et Authentification

L'authentification est prise en charge par [AuthService.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/service/AuthService.java:11). Cette classe permet :

- la connexion d'un utilisateur existant ;
- la création d'un nouveau compte ;
- la validation des champs saisis ;
- le hashage des mots de passe avec BCrypt avant stockage.

L'utilisation de BCrypt constitue un point positif important, car elle évite de stocker les mots de passe en clair. Lors de la connexion, le mot de passe fourni est comparé au mot de passe hashé enregistré en base.

La gestion des utilisateurs repose principalement sur [UtilisateurDAO.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/dao/UtilisateurDAO.java:8), qui permet de créer, rechercher et lister les utilisateurs.

## 8. Messagerie Privée

La messagerie privée fait partie des fonctionnalités les plus avancées du projet. Elle permet :

- l'envoi de messages texte en temps réel ;
- la réception immédiate des messages ;
- l'affichage de l'historique ;
- le marquage des messages lus ;
- des opérations de suppression.

Le serveur route les messages privés à travers [ChatServer.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/server/ChatServer.java:156). Le client réseau utilise [NetworkClient.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/client/NetworkClient.java:1) pour l'envoi et la réception.

L'interface principale de messagerie est pilotée par [MainChatController.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/client/controller/MainChatController.java:1), qui centralise une grande partie de la logique fonctionnelle.

## 9. Gestion Des Contacts Et Des Statuts

Le projet inclut un système de gestion des contacts ainsi qu'un suivi de la présence des utilisateurs.

Les principales capacités disponibles sont :

- ajout de contacts ;
- suppression de contacts ;
- chargement de la liste des contacts ;
- affichage du statut en ligne ou hors ligne.

Le serveur diffuse également la liste des utilisateurs connectés et les changements de statut. Cela améliore l'expérience utilisateur en donnant une vision instantanée des personnes disponibles.

## 10. Groupes De Discussion

Le projet prend en charge les groupes de discussion, avec des entités et des couches DAO dédiées, notamment :

- `Groupe`
- `GroupeMembre`
- `GroupeDAO`
- `GroupeMembreDAO`

Dans l'interface, des éléments spécifiques aux groupes sont présents dans [MainChatController.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/client/controller/MainChatController.java:1), ainsi que dans plusieurs fichiers FXML et contrôleurs associés.

Le système de groupes permet globalement :

- la création d'un groupe ;
- la récupération de la liste des groupes ;
- l'ajout et la suppression de membres ;
- l'envoi de messages de groupe ;
- l'accès à l'historique d'un groupe.

Cette partie montre que le projet dépasse le simple cadre d'un chat individuel.

## 11. Réunions Et Communication Temps Réel

L'un des aspects les plus intéressants de Chatt-App est la présence d'une architecture dédiée aux réunions de groupe et aux médias temps réel.

La gestion des réunions côté serveur est assurée par [MeetingManager.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/server/MeetingManager.java:17). Cette classe permet notamment :

- de démarrer une réunion ;
- de vérifier l'appartenance d'un utilisateur à un groupe ;
- d'ajouter et retirer des participants ;
- de maintenir une session active en mémoire ;
- d'informer les participants sur l'état de la réunion ;
- de fournir les informations de connexion aux flux médias.

La persistance des réunions est assurée par [ReunionDAO.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/dao/ReunionDAO.java:9), qui prend en charge la création des réunions, la récupération de l'historique, la fermeture d'une réunion et la gestion des participants.

## 12. Appels Audio Et Transmission UDP

Le projet contient une implémentation dédiée à la transmission audio UDP dans [AudioTransmissionService.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/client/audio/AudioTransmissionService.java:13).

Cette classe remplit plusieurs rôles :

- capture audio locale ;
- envoi des trames audio via UDP ;
- réception des paquets audio ;
- lecture du son reçu ;
- support d'un en-tête spécial pour l'identification de réunion et d'utilisateur.

Le choix d'UDP est logique pour l'audio temps réel, car il permet de réduire la latence par rapport à TCP.

## 13. Vidéo Et Réunions Vidéo

La vidéo pour les réunions s'appuie notamment sur [MeetingVideoCapture.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/client/video/MeetingVideoCapture.java:13). Ce composant gère :

- l'envoi des images vidéo compressées ;
- la réception des images distantes ;
- l'association des flux avec l'identité de l'expéditeur ;
- le transport via UDP.

Le relais côté serveur est assuré par [UDPRelayServer.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/server/UDPRelayServer.java:12), qui reçoit les datagrammes d'un participant puis les redistribue aux autres membres de la réunion.

Cette approche est adaptée à un projet pédagogique, car elle permet d'obtenir un fonctionnement concret sans la complexité d'une pile WebRTC complète.

## 14. Base De Données

Le schéma relationnel du projet est relativement complet. Il est défini dans [schema.sql](C:/Users/pc/Music/Chatt-App/src/main/resources/sql/schema.sql:1).

Les principales tables sont :

- `utilisateur`
- `contact`
- `groupe`
- `groupe_membre`
- `conversation`
- `reunion`
- `reunion_participant`
- `message`
- `fichier_media`
- `vocal`
- `video`
- `appel`
- `notification`
- `connexion`

Ce schéma permet de gérer non seulement les conversations privées, mais aussi les groupes, les réunions, les appels, les fichiers multimédias et les états de suppression.

## 15. Migration Et Évolution Du Schéma

Le fichier [DbMigration.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/scratch/DbMigration.java:10) montre que le projet a continué à évoluer après la création du schéma initial.

La migration ajoute notamment :

- le support des citations de messages ;
- un texte d'aperçu de réponse ;
- un lien vers le message parent ;
- des options de groupe comme `ephemeral_timer` ;
- des colonnes liées au silence et aux favoris ;
- une table `appel_planifie`.

Cela indique une volonté d'enrichir progressivement l'application avec des fonctionnalités avancées.

## 16. Interface Graphique

L'interface utilisateur est construite avec JavaFX et FXML. On trouve plusieurs vues dans `src/main/resources/fxml`, par exemple :

- `login.fxml`
- `main-chat-view.fxml`
- `group-view.fxml`
- `meeting-window.fxml`
- `video-call.fxml`
- `profile-view.fxml`

Plusieurs feuilles de style CSS sont également présentes, ce qui montre un effort de personnalisation visuelle.

L'interface semble relativement riche, avec gestion des onglets, des conversations, des groupes, des boutons d'appel, de la recherche de contacts et des interactions multimédias.

## 17. Points Forts Du Projet

Les principaux points forts observés sont les suivants :

- architecture claire en plusieurs couches ;
- séparation client, serveur, DAO et modèles ;
- utilisation de technologies pertinentes et actuelles ;
- authentification avec sécurisation des mots de passe ;
- persistance des données bien structurée ;
- présence d'un système de groupes ;
- prise en charge des réunions ;
- base réelle pour l'audio et la vidéo temps réel ;
- compilation Maven réussie ;
- projet ambitieux et techniquement riche pour un contexte académique.

## 18. Limites Et Faiblesses Actuelles

Malgré ses qualités, le projet présente plusieurs limites importantes :

- absence de tests automatisés ;
- forte concentration de logique dans certaines classes volumineuses comme `MainChatController` ;
- configuration de la base de données codée en dur dans [DatabaseConnection.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/dao/DatabaseConnection.java:12) ;
- dépendance à un environnement local MySQL ;
- protocole de sérialisation maison plus fragile qu'une solution standard comme JSON ;
- nécessité de valider plus profondément la stabilité réelle des appels audio et vidéo en conditions d'utilisation réelles ;
- documentation interne encore inégale selon les modules.

L'absence de dossier `src/test` montre clairement que les tests unitaires et tests d'intégration n'ont pas encore été mis en place.

## 19. Évaluation De L'État Actuel

Au vu du code actuellement présent, le projet est plus avancé que ce que certains anciens documents du dépôt indiquent.

L'état actuel peut être résumé ainsi :

- authentification : bien avancée ;
- messagerie privée : bien avancée ;
- persistance en base : bien avancée ;
- contacts et statuts : bien avancés ;
- groupes : présents et exploitables ;
- réunions : structure existante et cohérente ;
- audio/vidéo : base technique réelle, mais nécessitant validation fonctionnelle complète ;
- tests : absents ;
- industrialisation : encore faible.

Dans son état actuel, Chatt-App peut être considéré comme un projet fonctionnel en développement avancé, mais pas encore totalement stabilisé.

## 20. Recommandations D'Amélioration

Pour améliorer le projet, les actions prioritaires recommandées sont :

1. ajouter des tests unitaires et des tests d'intégration ;
2. externaliser la configuration de la base de données ;
3. refactoriser les contrôleurs trop volumineux ;
4. standardiser davantage la sérialisation des messages ;
5. renforcer la gestion des erreurs réseau ;
6. tester les appels audio et vidéo sur plusieurs machines ;
7. compléter la documentation technique et fonctionnelle ;
8. préparer un packaging exécutable propre pour le client et le serveur.

Ces améliorations permettraient de faire évoluer le projet d'un bon prototype académique vers une application plus robuste.

## 21. Conclusion

Chatt-App est un projet complet, ambitieux et techniquement intéressant. Il ne se limite pas à une simple application de chat texte, mais propose une véritable plateforme de communication avec authentification, conversations privées, groupes, persistance en base de données, échanges multimédias et réunions de groupe.

Le projet présente de très bonnes bases architecturales et un niveau d'avancement significatif. Il reste cependant du travail sur la qualité logicielle, les tests, la stabilité réseau et la finalisation de certaines fonctionnalités pour atteindre un niveau de maturité plus élevé.

Dans l'ensemble, Chatt-App constitue un très bon projet de développement logiciel, particulièrement pertinent dans un cadre universitaire, car il mobilise des compétences en programmation orientée objet, interfaces graphiques, réseaux, bases de données, communication temps réel et conception logicielle.