# Explication Détaillée Des Fonctions De Chatt-App

## 1. But De Ce Document

Ce document explique les fonctions existantes dans `Chatt-App` en se basant sur le code réel du projet.

Vu la taille de l'application, il est important de distinguer deux catégories :

- les **fonctions métier importantes**, qui portent la logique réelle de l'application ;
- les **fonctions simples** comme les getters, setters, accesseurs et petites méthodes d'affichage, qui sont nombreuses mais répétitives.

Dans ce document, les fonctions métier sont détaillées une par une. Les getters/setters des modèles sont résumés ensemble, car leur rôle est standard : lire ou modifier les attributs des objets.

---

## 2. Vue Globale Des Fonctionnalités Existantes

Le projet `Chatt-App` contient les grandes fonctionnalités suivantes :

- lancement du client JavaFX ;
- connexion et inscription ;
- communication client-serveur via TCP ;
- envoi et réception de messages privés ;
- gestion de l'historique ;
- gestion des contacts ;
- gestion des groupes ;
- appels audio et vidéo ;
- réunions de groupe ;
- relais UDP pour médias ;
- persistance MySQL via DAO ;
- stockage et lecture de médias ;
- statuts de présence et suivi des connexions.

---

## 3. Fonctions Du Côté Client

## 3.1 `ChatClientApp`

Fichier : [ChatClientApp.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/client/ChatClientApp.java:14)

### `start(Stage stage)`

Cette fonction démarre l'application JavaFX. Elle reçoit la fenêtre principale `Stage`, la stocke dans une variable statique, définit le titre initial et appelle l'affichage de la vue de connexion.

Rôle principal :

- initialiser la fenêtre principale ;
- lancer l'interface utilisateur ;
- faire de l'écran de connexion le premier écran affiché.

### `showLoginView()`

Cette fonction charge le fichier FXML de connexion `login.fxml`, crée une nouvelle scène, applique la feuille de style CSS et affiche la fenêtre.

Rôle principal :

- ouvrir l'écran de login/register ;
- vérifier que le fichier FXML existe ;
- appliquer le thème graphique.

### `showMainChat(NetworkClient networkClient, String username, int userId)`

Cette fonction ouvre l'interface principale de discussion après authentification réussie.

Rôle principal :

- charger `main-chat-view.fxml` ;
- créer la scène principale de chat ;
- transmettre le client réseau, le nom d'utilisateur et l'identifiant au contrôleur ;
- connecter les événements de redimensionnement de fenêtre.

### `getPrimaryStage()`

Retourne la fenêtre principale de l'application pour qu'elle puisse être réutilisée ailleurs dans l'application.

### `main(String[] args)`

Point d'entrée Java standard. Cette méthode appelle `launch(args)` pour démarrer JavaFX.

---

## 3.2 `MainLauncher`

Fichier : [MainLauncher.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/client/MainLauncher.java:1)

### `main(String[] args)`

Fonction de lancement auxiliaire. Elle sert surtout à contourner certains problèmes de lancement JavaFX selon le contexte Maven/module-path.

---

## 3.3 `LoginController`

Fichier : [LoginController.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/client/controller/LoginController.java:1)

### `initialize()`

Initialise le contrôleur au chargement de la vue. Configure notamment les comportements liés aux champs et aux boutons.

### `setupPasswordToggle(PasswordField pf, TextField tf, CheckBox cb)`

Permet d'afficher ou masquer le mot de passe. Cette fonction synchronise un `PasswordField`, un `TextField` visible et une case à cocher.

### `onLogin()`

Cette fonction est déclenchée quand l'utilisateur veut se connecter.

Elle réalise plusieurs étapes :

- lire l'hôte et le port ;
- lire le nom d'utilisateur et le mot de passe ;
- valider les champs ;
- créer le client réseau ;
- se connecter au serveur ;
- envoyer la demande de login.

### `onRegister()`

Déclenchée lors de l'inscription. Elle vérifie :

- le nom d'utilisateur ;
- l'email ;
- le mot de passe ;
- le port et l'hôte ;

puis envoie la demande d'inscription au serveur.

### `parsePort(String portStr, Label errorLabel)`

Convertit le port saisi sous forme de texte en entier. Si le port n'est pas valide, un message d'erreur est affiché.

### `connectAndSend(String host, int port, Label errorLabel, Runnable sendAction)`

Fonction utilitaire qui centralise :

- la création du `NetworkClient` ;
- la connexion TCP ;
- l'envoi de l'action voulue (`login` ou `register`) ;
- la gestion des erreurs de connexion.

### `showError(Label label, String msg)`

Affiche un message d'erreur dans le label prévu.

### `isValidEmail(String email)`

Valide le format de l'email grâce à une expression régulière.

### `setButtonsDisabled(boolean disabled)`

Active ou désactive les boutons de l'écran afin d'éviter des clics multiples pendant une opération réseau.

---

## 3.4 `NetworkClient`

Fichier : [NetworkClient.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/client/NetworkClient.java:1)

Cette classe est l'un des noyaux du client. Elle gère toute la communication avec le serveur.

### Fonctions de configuration des callbacks

Exemples :

- `setOnMessageReceived`
- `setOnAuthSuccess`
- `setOnAuthFail`
- `setOnUserListReceived`
- `setOnContactListReceived`
- `setOnUserStatusChanged`
- `setOnHistoryReceived`
- `setOnIncomingCall`
- `setOnCallAnswered`
- `setOnCallRejected`
- `setOnMeetingInvite`
- `setOnMeetingStarted`
- `setOnMeetingEnded`
- `setOnMeetingParticipantJoined`
- `setOnMeetingParticipantLeft`
- `setOnMeetingInfo`
- `setOnMeetingParticipants`
- `setOnMeetingNonParticipantsResponse`
- `setOnGroupCreated`
- `setOnGroupListResponse`
- `setOnGroupMessage`
- `setOnGroupMemberAdded`
- `setOnGroupMemberRemoved`
- `setOnGroupMembersResponse`
- `setOnGroupHistoryResponse`

Toutes ces fonctions ont le même rôle global : enregistrer une action à exécuter quand un type de message particulier arrive depuis le serveur.

### `connect()`

Ouvre la socket TCP vers le serveur et lance le thread d'écoute.

### `login(String username, String password)`

Construit un `ChatMessage` de type `LOGIN` et l'envoie au serveur.

### `register(String username, String password, String email)`

Construit un message `REGISTER` contenant les données d'inscription et l'envoie au serveur.

### `requestUserList()`

Demande au serveur la liste des utilisateurs connectés.

### `addContact(String contactUsername)`

Envoie une requête d'ajout de contact.

### `deleteContact(String contactUsername)`

Demande la suppression logique d'un contact.

### `sendDeleteContact(int contactId)`

Envoie une suppression basée sur l'identifiant du contact.

### `requestContacts()`

Charge la liste de contacts de l'utilisateur courant.

### `requestHistory(String otherUser)`

Demande l'historique de conversation privée avec un autre utilisateur.

### `clearPrivateChat(String otherUser)`

Demande un effacement local d'une conversation privée pour l'utilisateur courant.

### `deletePrivateChatForEveryone(String otherUser)`

Demande une suppression globale du contenu d'une conversation privée.

### `clearMessageForMe(int messageId)`

Efface un message seulement pour l'utilisateur courant.

### `deleteMessageForEveryone(int messageId)`

Supprime un message pour tous les participants.

### `sendDeleteMessage(int messageId)`

Version bas niveau de l'envoi de suppression d'un message.

### `markMessageRead(int messageId)`

Informe le serveur qu'un message a été lu.

### Fonctions liées aux réunions

#### `startGroupMeeting(int groupId, String meetingType)`

Démarre une réunion audio ou vidéo dans un groupe.

#### `joinMeetingByGroup(int groupId)`

Rejoint une réunion à partir du groupe.

#### `joinMeeting(int meetingId)`

Rejoint une réunion en utilisant directement son identifiant.

#### `sendMeetingJoin(int meetingId)`

Méthode interne qui construit le message de participation à une réunion.

#### `fillUdpPorts(ChatMessage msg)`

Ajoute dans le message les ports UDP locaux audio/vidéo pour que le serveur sache où relayer les flux.

#### `syncMeetingMediaPorts(int meetingId)`

Resynchronise les ports médias quand la réunion a déjà commencé mais que les ports ont changé ou doivent être redéclarés.

#### `leaveMeeting(int meetingId)`

Quitte une réunion.

#### `endMeeting(int meetingId)`

Termine une réunion, généralement par l'initiateur.

#### `startMeetingAudio(...)`

Démarre le service audio UDP côté client pour une réunion.

#### `startMeetingVideo(...)`

Démarre le service vidéo UDP côté client pour une réunion.

#### `sendMeetingAudioFrame(byte[] frame)`

Envoie une trame audio vers le relais de réunion.

#### `sendMeetingVideoFrame(byte[] frame)`

Envoie une trame vidéo vers le relais de réunion.

#### `stopMeetingMedia()`

Arrête les services médias actifs.

#### `sendMicState(int meetingId, boolean micOn)`

Informe les autres participants de l'état du micro.

#### `sendVideoState(int meetingId, boolean videoOn)`

Informe les autres participants de l'état de la caméra.

#### `setMeetingMicEnabled(boolean enabled)`

Active ou désactive la capture micro côté client.

#### `setMeetingPlaybackVolume(double volume)`

Change le volume de lecture audio.

#### `resolveMeetingRelayHost(String advertisedHost)`

Résout intelligemment l'adresse du relais UDP, utile quand le serveur annonce une IP locale ou générique.

### Fonctions liées aux groupes

#### `createGroup(String nom, String description)`

Crée un groupe simple.

#### `createGroup(String name, String description, List<String> members)`

Crée un groupe avec une liste de membres initiale.

#### `sendGroupMessage(int groupId, String content)`

Envoie un message texte dans un groupe.

#### `sendGroupMedia(int groupId, MessageType type, String fileName, byte[] data)`

Envoie un média dans un groupe.

#### `addGroupMember(int groupId, String pseudo)`

Ajoute un membre à un groupe.

#### `removeGroupMember(int groupId, String pseudo)`

Supprime un membre d'un groupe.

#### `leaveGroup(int groupId)`

Quitte un groupe.

#### `requestGroupList()`

Charge la liste des groupes de l'utilisateur.

#### `requestGroupHistory(int groupId)`

Charge l'historique d'un groupe.

#### `clearGroupChat(int groupId)`

Efface localement l'historique d'un groupe.

#### `deleteGroupChatForEveryone(int groupId)`

Supprime tous les messages du groupe pour tous.

#### `requestGroupMembers(int groupId)`

Charge la liste des membres d'un groupe.

### `send(ChatMessage msg)`

Fonction centrale d'envoi. Elle sérialise `ChatMessage` et l'écrit sur la socket TCP.

### `close()`

Ferme la connexion réseau et nettoie les ressources.

### `startListenerThread()`

Lance le thread qui écoute continuellement les messages entrants du serveur.

### `dispatch(ChatMessage msg)`

Analyse le type de message reçu puis déclenche le callback correspondant.

---

## 3.5 `MainChatController`

Fichier : [MainChatController.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/client/controller/MainChatController.java:1)

Cette classe est le plus gros contrôleur de l'application. Elle centralise l'interface principale.

Comme elle contient beaucoup de fonctions, leur explication est organisée par catégorie.

### Fonctions d'initialisation

#### `initialize()`

Prépare l'interface dès le chargement :

- configure les boutons ;
- prépare l'affichage des messages ;
- prépare les onglets privés, groupes et contacts ;
- configure la barre de sélection de messages.

#### `init(NetworkClient networkClient, String username, int userId)`

Injecte les dépendances de session et branche tous les callbacks réseau :

- messages reçus ;
- statuts ;
- appels entrants ;
- réponses d'appel ;
- réunions ;
- groupes ;
- historique ;
- liste de contacts.

Cette méthode démarre réellement le fonctionnement interactif de l'écran principal.

#### `onWindowResize(double width, double height)`

Rend l'interface plus responsive en ajustant la taille des panneaux.

### Fonctions liées à l'interface groupes/contacts

#### `buildGroupTabContent()`

Construit dynamiquement le contenu visuel de l'onglet groupes.

#### `buildContactsTabContent()`

Construit dynamiquement le contenu visuel de l'onglet contacts.

#### `showPrivateCenter()`

Replace l'affichage central sur la zone de discussion privée.

#### `registerGroupCallbacks()`

Branche les actions à exécuter quand le serveur renvoie :

- liste de groupes ;
- création de groupe ;
- historique de groupe ;
- ajout/suppression de membres ;
- messages de groupe.

### Fonctions de gestion des groupes

#### `addGroup(int id, String name)`

Ajoute localement un groupe dans l'interface.

#### `removeGroupLocally(int groupId)`

Retire un groupe localement de l'interface et des structures mémoire.

#### `openGroupChat(int groupId, String groupName)`

Ouvre l'affichage d'un groupe particulier et charge son historique.

#### `addGroupMessage(ChatMessage msg, boolean fromHistory)`

Transforme un message reçu en message d'interface `UiMessage` et l'ajoute à la conversation de groupe.

#### `kindForGroup(MessageType type)`

Associe le type réseau du message à un type visuel d'affichage.

#### `safeMessageType(String raw)`

Convertit une chaîne en `MessageType` sans provoquer d'erreur fatale.

#### `saveIncomingGroupMedia(UiMessage.Kind kind, String name, byte[] data)`

Enregistre un média de groupe reçu sur disque.

#### `parseGroupEntry(String raw)`

Décode une ligne texte représentant un groupe.

### Fonctions de recherche et contacts

#### `setupSearchContactAutoCompletion()`

Prépare l'autocomplétion sur la barre de recherche de contacts.

#### `onAddContact()`

Déclenchée lors de l'ajout d'un contact. Elle valide la saisie puis envoie la requête au serveur.

#### `updateContactList(List<String> contacts)`

Met à jour la liste des contacts visible dans l'interface.

#### `setupContactCellFactory()`

Personnalise l'apparence graphique des cellules de la liste de contacts.

#### `onUserStatusChanged(ChatMessage msg)`

Réagit aux changements de statut des utilisateurs.

#### `updateOnlineUsers(List<String> users)`

Met à jour l'état visuel des utilisateurs actuellement en ligne.

### Fonctions de messagerie privée

#### `openPrivateChat(String username)`

Ouvre une conversation privée avec un utilisateur.

#### `onSend()`

Fonction déclenchée à l'envoi d'un message :

- identifie la cible courante ;
- lit le texte saisi ;
- construit le message ;
- l'ajoute visuellement ;
- l'envoie via `NetworkClient`.

#### `onMessageReceived(ChatMessage msg)`

Fonction centrale de réception côté interface. Elle transforme les messages réseau en messages affichables, met à jour la conversation courante et gère les messages non lus.

#### `onHistoryReceived(ChatMessage msg)`

Reçoit et reconstruit l'historique envoyé par le serveur.

### Fonctions liées aux appels

#### `onVoiceCall()`

Déclenche un appel vocal privé vers le contact sélectionné.

#### `onVideoCall()`

Déclenche un appel vidéo privé vers le contact sélectionné.

#### `handleIncomingCall(ChatMessage msg)`

Traite une demande d'appel entrant : affichage, acceptation ou refus.

#### `handleCallAccepted(ChatMessage msg)`

Réagit lorsqu'un correspondant accepte l'appel.

#### `handleCallRejected(ChatMessage msg)`

Réagit lorsqu'un appel est refusé.

### Fonctions d'audio local et d'enregistrement

#### `updateRecordButtonState()`

Met à jour l'état graphique du bouton d'enregistrement audio.

#### `toggleAudioRecording()`

Lance ou arrête l'enregistrement audio local.

#### `startAudioRecording()`

Démarre la capture micro et la visualisation d'onde.

#### `stopAudioRecording()`

Arrête la capture audio et prépare le message audio à envoyer.

### Fonctions de sélection de messages

Cette zone du contrôleur gère la sélection multiple des messages pour :

- copier ;
- transférer ;
- supprimer ;
- sélectionner tout ;
- sortir du mode sélection.

Les fonctions typiques sont :

- `copySelectedMessages()`
- `forwardSelectedMessages()`
- `deleteSelectedMessagesForEveryone()`
- `selectAllMessages()`
- `exitSelectionMode()`

Leur rôle est purement orienté interface et actions utilisateur.

### Fonctions de lecture/affichage utilitaires

Le contrôleur contient aussi plusieurs petites fonctions de support, comme :

- formatage d'heure ;
- choix de couleur d'avatar ;
- affichage de fenêtres d'information ;
- ouverture de fichiers reçus ;
- adaptation de l'UI.

Elles sont utiles mais ne portent pas la logique métier principale.

---

## 3.6 Appels audio/vidéo côté client

## `VoiceCallSession`

Fichier : [VoiceCallSession.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/client/voice/VoiceCallSession.java:1)

### `start()`

Démarre une session d'appel vocal :

- initialise capture et lecture audio ;
- prépare l'envoi vers le réseau ;
- démarre les composants audio.

### `playRemoteAudio(byte[] data)`

Joue les données audio reçues depuis l'autre participant.

### `setMicrophoneMuted(boolean microphoneMuted)`

Coupe ou réactive le micro local.

### `setSpeakerEnabled(boolean speakerEnabled)`

Active ou désactive le haut-parleur.

### `stop()`

Arrête complètement la session d'appel vocal.

## `VoiceCallWindow`

Fichier : [VoiceCallWindow.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/client/voice/VoiceCallWindow.java:36)

### `open(...)`

Ouvre la fenêtre graphique d'appel vocal.

### `close()`

Ferme la fenêtre active.

Les autres fonctions internes de cette classe mettent à jour :

- le bouton micro ;
- le bouton haut-parleur ;
- le statut visuel ;
- le chronomètre d'appel.

## `VideoCallWindow`

Fichier : [VideoCallWindow.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/client/video/VideoCallWindow.java:15)

### `open(NetworkClient client, String me, String other, boolean caller)`

Ouvre la fenêtre d'appel vidéo.

### `closeCurrent()`

Ferme la fenêtre vidéo actuellement ouverte.

## `VideoCallController`

Fichier : [VideoCallController.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/client/video/VideoCallController.java:70)

### `init(NetworkClient client, String me, String other)`

Initialise la session d'appel vidéo : nom local, distant, client réseau, capture et affichage.

### `receiveFrame(byte[] data)`

Reçoit une image vidéo distante et l'affiche.

### `receiveAudio(byte[] data)`

Reçoit l'audio d'un appel vidéo.

### `onToggleMic()`

Active ou désactive le micro.

### `onToggleSpeaker()`

Active ou désactive le haut-parleur.

### `onToggleVideo()`

Active ou désactive la caméra.

### `onEndCall()`

Termine l'appel vidéo.

---

## 3.7 Services audio et vidéo

## `AudioCaptureService`

Fichier : [AudioCaptureService.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/client/audio/AudioCaptureService.java:12)

Rôle global : capturer l'audio du microphone.

Fonctions importantes :

- `setOnAudioCaptured` : enregistre le callback appelé à chaque bloc audio capturé ;
- `start()` : démarre la capture avec le format par défaut ;
- `start(AudioFormat preferredFormat, Consumer<byte[]> callback)` : démarre avec un format audio choisi ;
- `findBestDuplexFormat()` : cherche un format compatible pour émission/réception ;
- `findSupportedCaptureFormat()` : cherche un format réellement supporté par la machine ;
- `stop()` : arrête la capture ;
- `getFormat()` : retourne le format utilisé ;
- `isRunning()` : dit si la capture est active ;
- `getAmplitudeData()` : fournit les amplitudes calculées, utiles pour visualiser l'onde ;
- `calculateAmplitude(...)` : calcule le niveau sonore d'un bloc audio.

## `AudioPlaybackService`

Fichier : [AudioPlaybackService.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/client/audio/AudioPlaybackService.java:12)

Rôle global : lire les données audio reçues.

Fonctions importantes :

- `start()` / `start(AudioFormat preferredFormat)` : initialise la sortie audio ;
- `playAudio(byte[] audioData)` : joue un buffer audio ;
- `playAudio(byte[] audioData, AudioFormat sourceFormat)` : joue un buffer avec conversion de format si besoin ;
- `setVolume(double volume)` : règle le volume ;
- `stop()` : arrête la lecture ;
- `isRunning()` : indique si la lecture est active.

## `AudioTransmissionService`

Fichier : [AudioTransmissionService.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/client/audio/AudioTransmissionService.java:14)

Rôle global : transmettre l'audio en UDP.

Fonctions importantes :

- `initiate(...)` : crée la socket UDP, démarre capture et lecture, puis lance le thread de réception ;
- `sendAudioFrame(byte[] audioData)` : envoie explicitement un paquet audio ;
- `getLocalPort()` : retourne le port local UDP utilisé ;
- `stop()` : arrête la transmission ;
- `isRunning()` : indique si le service fonctionne ;
- `setCaptureEnabled(boolean enabled)` : coupe ou réactive la capture ;
- `setPlaybackVolume(double volume)` : change le volume de lecture ;
- `setOnAudioReceived(Consumer<byte[]> callback)` : permet à un composant externe de traiter l'audio reçu.

## `MeetingVideoCapture`

Fichier : [MeetingVideoCapture.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/client/video/MeetingVideoCapture.java:13)

Rôle global : envoyer et recevoir la vidéo de réunion via UDP.

Fonctions importantes :

- `setOnRemoteFrame(...)` : définit le callback à appeler quand une image distante arrive ;
- `start(...)` : démarre le socket vidéo UDP ;
- `sendFrame(byte[] jpegFrame)` : envoie une image JPEG ;
- `stop()` : arrête la réception/émission ;
- `getLocalPort()` : retourne le port local utilisé.

## `VideoCaptureService`

Fichier : [VideoCaptureService.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/client/util/VideoCaptureService.java:16)

Rôle global : capturer la webcam locale.

Fonctions importantes :

- `setOnFrameCaptured` : définit le traitement d'une image capturée ;
- `start()` : démarre la caméra ;
- `stop()` : arrête la caméra ;
- `switchCamera(boolean front)` : change de caméra si plusieurs sont disponibles.

## `MeetingVideoDisplay`

Fichier : [MeetingVideoDisplay.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/client/video/MeetingVideoDisplay.java:19)

Rôle global : organiser l'affichage des vidéos de participants.

Fonctions importantes :

- `setGrid(GridPane grid)` : injecte la grille visuelle ;
- `addParticipant(String participantId, String displayName)` : crée une case vidéo ;
- `removeParticipant(String participantId)` : retire la vidéo d'un participant ;
- `clearParticipants()` : vide complètement la grille ;
- `updateFrame(String participantId, byte[] jpegFrame)` : met à jour l'image affichée pour un participant.

---

## 4. Fonctions Du Côté Serveur

## 4.1 `ChatServer`

Fichier : [ChatServer.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/server/ChatServer.java:20)

Cette classe représente le serveur principal.

### `main(String[] args)`

Démarre le serveur TCP, lance le relais UDP, écoute les connexions et crée un `ClientHandler` par client.

### `getGroupManager()`, `getMeetingManager()`, `getUdpRelayServer()`

Permettent d'accéder aux composants serveur secondaires.

### `getServerHost()`, `getUdpAudioPort()`, `getUdpVideoPort()`

Exposent les informations réseau du relais UDP.

### `registerClient(String username, int userId, ClientHandler handler)`

Enregistre une nouvelle session client :

- mémorise le client dans les structures du serveur ;
- marque l'utilisateur en ligne dans la base ;
- diffuse le statut ;
- actualise la liste des utilisateurs.

### `removeClient(String username, int userId, ClientHandler handler)`

Retire une session du serveur et met à jour l'état de connexion.

### `broadcastUserList()`

Diffuse à tous les clients la liste actuelle des utilisateurs connectés.

### `broadcastUserStatus(String username, String status)`

Diffuse un changement de statut pour un utilisateur.

### `broadcastToGroup(int groupeId, ChatMessage msg)`

Diffuse un message à tous les membres d'un groupe.

### `broadcastToGroupExcept(int groupeId, ChatMessage msg, int exceptUserId)`

Diffuse à tous les membres sauf un utilisateur donné.

### `sendToUserId(int userId, ChatMessage msg)`

Envoie directement un message à un utilisateur identifié par son `userId`.

### `handleMessage(ChatMessage msg, ClientHandler from)`

Point central de routage des messages :

- détermine la catégorie du message ;
- envoie vers la logique privée, appel ou autre ;
- relaie selon le type.

### `routePrivate(ChatMessage msg, ClientHandler from)`

Route un message privé vers le destinataire et le renvoie aussi vers les autres sessions du même expéditeur.

### `forwardToTarget(ChatMessage msg)`

Envoie simplement un message à la cible `to`.

### `handleCallMessage(ChatMessage msg, ClientHandler from)`

Traite les messages liés aux appels.

### `isPrivateMessage(MessageType type)`

Indique si le type correspond à une conversation privée.

### `isCallMessage(MessageType type)`

Indique si le type correspond à un message d'appel ou de réunion liée à l'appel.

### `handleCallRequest(ChatMessage msg, ClientHandler from)`

Vérifie si le destinataire est connecté, puis transmet l'appel entrant.

### `handleCallAnswer(ChatMessage msg, ClientHandler from)`

Construit les informations nécessaires à l'établissement de l'appel après acceptation.

### `shutdown()`

Arrête proprement le relais UDP.

---

## 4.2 `ClientHandler`

Fichier : [ClientHandler.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/server/ClientHandler.java:1)

Cette classe est le vrai cœur du serveur applicatif. Chaque client connecté possède sa propre instance.

### `run()`

Boucle principale du thread :

- lit les lignes de la socket ;
- désérialise les messages ;
- appelle les méthodes de traitement adaptées.

### `processLine(String line)`

Analyse une ligne reçue, la convertit en `ChatMessage`, puis appelle le bon gestionnaire selon le type.

### Fonctions d'authentification

- `handleLogin(ChatMessage msg)` : vérifie les identifiants et ouvre la session ;
- `handleRegister(ChatMessage msg)` : crée un compte puis renvoie une réponse ;
- `setupSession(Utilisateur user)` : initialise les données de session après connexion.

### Fonctions contacts

- `handleContactAdd(ChatMessage msg)` : ajoute un contact ;
- `handleContactLoad()` : charge la liste de contacts ;
- `handleContactDelete(ChatMessage msg)` : supprime un contact ;
- `handleDeleteContact(ChatMessage msg)` : version basée sur identifiant.

### Fonctions messagerie privée

- `persistAndRoute(ChatMessage msg)` : persiste le message puis le route ;
- `handleHistoryRequest(ChatMessage msg)` : reconstruit l'historique privé ;
- `handleClearPrivateChat(ChatMessage msg)` : efface l'historique privé pour un utilisateur ;
- `handleClearMessageForMe(ChatMessage msg)` : masque un message pour un utilisateur ;
- `handleMessageRead(ChatMessage msg)` : marque le message comme lu ;
- `handleDeleteMessageForEveryone(ChatMessage msg)` : supprime un message globalement ;
- `handleDeletePrivateChatForEveryone(ChatMessage msg)` : supprime tout le chat pour tous.

### Fonctions groupes

- `handleGroupCreate(ChatMessage msg)` : crée un groupe ;
- `handleGroupAddMember(ChatMessage msg)` : ajoute un membre ;
- `handleGroupRemoveMember(ChatMessage msg)` : retire un membre ;
- `handleGroupLeave(ChatMessage msg)` : permet à l'utilisateur de quitter le groupe ;
- `handleGroupList(ChatMessage msg)` : envoie la liste des groupes ;
- `handleGroupMembers(ChatMessage msg)` : renvoie les membres ;
- `handleGroupMessage(ChatMessage msg)` : persiste et diffuse un message de groupe ;
- `handleGroupHistoryRequest(ChatMessage msg)` : renvoie l'historique d'un groupe ;
- `handleClearGroupChat(ChatMessage msg)` : efface localement le chat de groupe ;
- `handleDeleteGroupChatForEveryone(ChatMessage msg)` : supprime tous les messages du groupe ;
- `handleGroupDelete(ChatMessage msg)` : supprime complètement un groupe.

### Fonctions réunions

- `handleMeetingStart(ChatMessage msg)` : démarre une réunion ;
- `handleMeetingJoin(ChatMessage msg)` : fait rejoindre une réunion ;
- `handleMeetingLeave(ChatMessage msg)` : fait quitter une réunion ;
- `handleMeetingEnd(ChatMessage msg)` : termine la réunion ;
- `handleMeetingParticipantsRequest(ChatMessage msg)` : renvoie la liste des participants ;
- `handleMeetingNonParticipantsRequest(ChatMessage msg)` : renvoie les membres du groupe absents de la réunion ;
- `handleMeetingInviteUser(ChatMessage msg)` : invite un membre à rejoindre ;
- `handleMicState(ChatMessage msg)` : diffuse l'état micro ;
- `handleVideoState(ChatMessage msg)` : diffuse l'état caméra ;
- `handleMeetingInfo(ChatMessage msg)` : met à jour ou relaie les informations réseau de réunion.

### Fonctions appels privés

- `handleCallRequest(ChatMessage msg)` : transmet une demande d'appel ;
- `handleCallAnswer(ChatMessage msg)` : traite l'acceptation ;
- `handleCallReject(ChatMessage msg)` : traite le refus ;
- `handleCallEnd(ChatMessage msg)` : traite la fin d'appel.

### Fonctions utilitaires

- `attachMediaIfPresent(...)` : ajoute les données média au message renvoyé ;
- `sendError(String message)` : renvoie un message d'erreur ;
- `sendGroupListToUser(int targetUserId)` : envoie la liste des groupes à un utilisateur ;
- `cleanup()` : ferme proprement la session quand la connexion se termine.

---

## 4.3 `GroupManager`

Fichier : [GroupManager.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/server/GroupManager.java:35)

Cette classe centralise la logique métier des groupes.

### `createGroup(...)`

Crée un groupe, son créateur et ses membres initiaux.

### `addMember(...)`

Ajoute un utilisateur dans un groupe après vérification des droits.

### `removeMember(...)`

Supprime un membre du groupe.

### `deleteGroup(...)`

Supprime complètement un groupe.

### `saveAndBroadcastGroupMessage(...)`

Persiste un message de groupe, gère éventuellement les médias associés, puis le diffuse aux membres.

### `getGroupHistory(...)`

Retourne l'historique d'un groupe.

### `getGroupsForUser(int userId)`

Renvoie tous les groupes d'un utilisateur.

### `leaveGroup(int groupeId, int userId)`

Retire un utilisateur du groupe.

### `getGroupIdsForUser(int userId)`

Renvoie seulement les identifiants des groupes d'un utilisateur.

### `getMembers(int groupeId, int requesterId)`

Retourne les membres d'un groupe après vérification d'accès.

### `getRole(int groupeId, int userId)`

Retourne le rôle d'un utilisateur dans le groupe.

### `getMemberIds(int groupeId)`

Renvoie les identifiants des membres.

### `isMember(...)`

Teste si l'utilisateur appartient au groupe.

### `isAdmin(...)`

Teste si l'utilisateur a un rôle d'administration.

### `serializeGroups(...)`

Transforme une liste de groupes en format texte transmissible au client.

### `serializeMessages(...)`

Transforme une liste de messages de groupe en chaîne de transfert.

---

## 4.4 `MeetingManager`

Fichier : [MeetingManager.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/server/MeetingManager.java:17)

Cette classe gère les réunions de groupe.

### `startMeeting(...)`

Crée ou réactive une réunion pour un groupe, vérifie les droits et ajoute l'initiateur comme participant.

### `joinMeeting(...)`

Ajoute un participant dans la réunion et l'enregistre dans le relais UDP.

### `updateParticipantMediaPorts(...)`

Met à jour les ports audio/vidéo d'un participant déjà présent.

### `leaveMeeting(...)`

Retire un participant, le désinscrit du relais UDP et ferme la réunion si elle devient vide.

### `endMeeting(...)`

Termine explicitement une réunion et en notifie les participants.

### `getActiveMeeting(int meetingId)`

Retourne une réunion active par identifiant.

### `getActiveMeetingForGroup(int groupeId)`

Retourne la réunion active associée à un groupe.

### `listParticipants(int meetingId)`

Retourne les participants en mémoire.

### `buildMeetingInfo(...)`

Construit le message contenant :

- les participants ;
- l'hôte du relais ;
- les ports UDP ;
- le type de réunion.

### `serializeParticipants(int meetingId)`

Transforme la liste des participants en chaîne sérialisée.

### `serializeNonParticipants(int meetingId, int groupeId)`

Retourne les membres du groupe qui ne participent pas encore à la réunion.

### `notifyParticipants(...)`

Diffuse un message à tous les participants d'une réunion.

---

## 4.5 `UDPRelayServer`

Fichier : [UDPRelayServer.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/server/UDPRelayServer.java:12)

Ce composant gère le relais UDP pour l'audio et la vidéo.

### `start()`

Ouvre les sockets UDP audio et vidéo puis lance les threads de relayage.

### `registerParticipant(...)`

Ajoute ou met à jour un participant pour une réunion.

### `unregisterParticipant(...)`

Retire un participant d'une réunion.

### `unregisterMeeting(int meetingId)`

Supprime complètement la réunion du relais.

### `getAudioPort()`, `getVideoPort()`, `getHost()`

Exposent les informations réseau du relais.

### `detectAdvertisedHost()`

Cherche automatiquement la meilleure adresse IP locale à annoncer aux clients.

### `relayLoop(DatagramSocket socket, MediaKind kind)`

Boucle serveur UDP :

- reçoit un paquet ;
- lit l'en-tête de réunion et d'utilisateur ;
- identifie l'expéditeur ;
- retransmet le flux aux autres participants.

### `close()`

Arrête le serveur UDP.

---

## 5. Fonctions De Persistance : Les DAO

Les DAO ont un rôle très important : ils isolent les requêtes SQL du reste de l'application.

---

## 5.1 `DatabaseConnection`

Fichier : [DatabaseConnection.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/dao/DatabaseConnection.java:1)

### `getInstance()`

Retourne l'instance unique de connexion.

### `getConnection()`

Fournit une connexion JDBC depuis le pool HikariCP.

### `close()`

Ferme le pool de connexions.

---

## 5.2 `UtilisateurDAO`

### `create(Utilisateur utilisateur)`

Insère un utilisateur en base.

### `findById(int id)`

Charge un utilisateur par identifiant.

### `findByUsername(String username)`

Charge un utilisateur à partir de son pseudo.

### `listAll()`

Retourne tous les utilisateurs.

### `searchByUsername(String query)`

Cherche les utilisateurs par pseudo.

### `getUsersByIds(List<Integer> ids)`

Retourne plusieurs utilisateurs à partir de plusieurs identifiants.

---

## 5.3 `ContactDAO`

### `addContact(int utilisateurId, int contactId)`

Ajoute une relation de contact.

### `contactExists(int utilisateurId, int contactId)`

Teste si la relation existe déjà.

### `deleteContact(int utilisateurId, int contactId)`

Supprime logiquement un contact.

### `hardDelete(int contactId, int userId)`

Supprime réellement l'entrée de contact.

### `findByUserId(int utilisateurId)` / `getContacts(int utilisateurId)`

Charge la liste des contacts d'un utilisateur.

---

## 5.4 `ConversationDAO`

### `createConversation(int user1Id, int user2Id)`

Crée une conversation privée entre deux utilisateurs.

### `findConversationBetween(int user1Id, int user2Id)`

Recherche la conversation existante entre deux utilisateurs.

### `getConversationsByGroupeId(int groupeId)`

Retourne les conversations liées à un groupe.

### `getParticipants(int conversationId)`

Retourne les participants d'une conversation.

---

## 5.5 `MessageDAO`

### `create(Message message)`

Insère un message privé ou standard.

### `saveGroupMessage(Message message)`

Insère un message de groupe.

### `getHistory(int conversationId)`

Charge l'historique brut d'une conversation.

### `getHistoryForUser(int conversationId, int utilisateurId)`

Charge l'historique filtré pour un utilisateur particulier.

### `getMessagesByGroupeId(...)`

Charge les messages d'un groupe.

### `markConversationCleared(...)`

Marque une conversation comme effacée pour un utilisateur.

### `markGroupCleared(...)`

Marque un groupe comme effacé pour un utilisateur.

### `deleteConversationMessages(int conversationId)`

Supprime tous les messages d'une conversation.

### `deleteGroupMessages(int groupeId)`

Supprime tous les messages d'un groupe.

### `findById(int messageId)`

Charge un message précis.

### `markMessageCleared(...)`

Marque un message comme supprimé localement.

### `deleteMessageForEveryone(...)`

Supprime un message pour tous les participants.

### `hardDelete(...)`

Supprime définitivement un message.

### `saveMessageSelection(...)`

Sauvegarde une sélection de messages.

### `getUnreadMessages(int userId)`

Retourne les messages non lus.

### `markAsRead(int messageId)`

Marque un message comme lu.

---

## 5.6 `GroupeDAO`

### `create(...)`

Crée un groupe.

### `findById(int id)`

Retourne un groupe par identifiant.

### `findByUtilisateurId(int utilisateurId)`

Charge les groupes d'un utilisateur.

### `update(...)`

Modifie le nom ou la description d'un groupe.

### `delete(int id)`

Supprime un groupe.

### `searchByNom(String query)`

Cherche des groupes par nom.

---

## 5.7 `GroupeMembreDAO`

### `addMember(...)`

Ajoute un membre dans un groupe.

### `removeMember(...)`

Supprime un membre.

### `getMembers(int groupeId)`

Retourne les membres complets d'un groupe.

### `getMemberIds(int groupeId)`

Retourne seulement leurs identifiants.

### `isMember(...)`

Teste l'appartenance.

### `getRole(...)`

Retourne le rôle du membre.

### `updateRole(...)`

Modifie le rôle.

### `getGroupIdsForUser(...)`

Retourne les groupes d'un utilisateur.

---

## 5.8 `ReunionDAO`

### `create(...)`

Crée une réunion en base.

### `findById(int id)`

Charge une réunion précise.

### `getHistoryByGroupeId(int groupeId)`

Retourne l'historique des réunions d'un groupe.

### `findActiveByGroupeId(int groupeId)`

Recherche la réunion encore active d'un groupe.

### `endMeeting(int reunionId)`

Clôture une réunion.

### `addParticipant(...)`

Ajoute un participant à une réunion.

### `removeParticipant(...)`

Retire un participant d'une réunion.

### `getCurrentParticipants(int reunionId)`

Retourne les participants présents.

---

## 5.9 DAO secondaires

### `AppelDAO`

Gère la persistance de l'historique des appels.

Fonctions principales :

- `create`
- `findById`
- `getAppelsByUserId`
- `getAppelsByReunionId`

### `FichierMediaDAO`

Gère les médias stockés.

Fonctions principales :

- `create`
- `findByMessageId`

### `ConnexionDAO`

Gère les sessions et statuts de connexion.

Fonctions principales :

- `setEnLigne`
- `setSessionClosed`

### `NotificationDAO`

Gère les notifications utilisateur.

Sa fonction principale est `create(Notification notif)`.

---

## 6. Service D'Authentification

## `AuthService`

Fichier : [AuthService.java](C:/Users/pc/Music/Chatt-App/src/main/java/org/example/tpchatjavafx/service/AuthService.java:11)

### `login(String username, String password)`

Cette fonction :

- vérifie que les champs ne sont pas vides ;
- recherche l'utilisateur par pseudo ;
- compare le mot de passe saisi avec le hash BCrypt enregistré ;
- retourne l'utilisateur si l'authentification réussit ;
- retourne `null` sinon.

### `register(String username, String password, String email)`

Cette fonction :

- vérifie les entrées ;
- s'assure que le pseudo est libre ;
- valide l'email ;
- hashe le mot de passe avec BCrypt ;
- crée l'utilisateur en base ;
- retourne un objet `RegisterResult` indiquant succès ou échec.

---

## 7. Modèles Et Fonctions Simples

Les classes de modèle suivantes contiennent surtout :

- des constructeurs ;
- des getters ;
- des setters ;
- parfois `equals`, `hashCode`, `toString`.

Classes concernées :

- `Utilisateur`
- `Message`
- `Conversation`
- `Groupe`
- `GroupeMembre`
- `Reunion`
- `Appel`
- `Notification`
- `FichierMedia`
- `Video`
- `Vocal`
- `UiMessage`
- `ChatMessage`

### Rôle de ces fonctions simples

- les **getters** servent à lire la valeur d'un attribut ;
- les **setters** servent à modifier la valeur d'un attribut ;
- `equals` sert à comparer deux objets ;
- `hashCode` sert à garantir une cohérence dans les collections Java ;
- `toString` sert à produire une représentation textuelle de l'objet.

### Cas particulier : `ChatMessage`

La classe `ChatMessage` a un rôle plus important que les autres modèles.

Fonctions clés :

- `serialize()` : transforme le message en chaîne transmissible sur le réseau ;
- `deserialize(String line)` : reconstruit un message à partir du texte reçu ;
- `parseIntSafe(...)` : convertit un entier sans planter si la valeur est invalide.

Cette classe est le support principal de communication entre client et serveur.

---

## 8. Résumé Final

Les fonctions les plus importantes de `Chatt-App` sont réparties ainsi :

- **lancement et navigation** : `ChatClientApp`, `MainLauncher`
- **connexion et inscription** : `LoginController`, `AuthService`
- **transport réseau client** : `NetworkClient`
- **chat principal et interface** : `MainChatController`
- **serveur TCP** : `ChatServer`, `ClientHandler`
- **groupes** : `GroupManager`, `GroupeDAO`, `GroupeMembreDAO`
- **réunions** : `MeetingManager`, `ReunionDAO`
- **audio et vidéo** : `AudioCaptureService`, `AudioPlaybackService`, `AudioTransmissionService`, `MeetingVideoCapture`, `VideoCaptureService`
- **persistance** : `MessageDAO`, `ConversationDAO`, `UtilisateurDAO`, `ContactDAO`, etc.

En résumé, `Chatt-App` est organisé autour de quelques grosses fonctions centrales :

- connecter l'utilisateur ;
- envoyer et recevoir des messages ;
- gérer les conversations privées et de groupe ;
- diffuser audio et vidéo ;
- gérer des réunions temps réel ;
- enregistrer toutes les données importantes en base.

Si tu veux, je peux maintenant faire une **version encore plus poussée**, avec :

1. une explication **classe par classe** ;
2. une explication **fonction par fonction avec schéma de flux** ;
3. une version en **français très simple** pour présentation orale.
