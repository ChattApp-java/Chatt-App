# Guide des corrections — Réunion groupe

## Fichiers modifiés

| Fichier | Rôle |
|---------|------|
| `MeetingController.java` | UI réunion, webcam (sarxos), audio local + relais UDP |
| `meeting-window.fxml` | Layout clair, `btnAddParticipant`, grille vidéo |
| `GroupController.java` | `startServices()`, retrait participant à la déconnexion |
| `NetworkClient.java` | `startAudioReceiving()` |
| `AudioTransmissionService.java` | Callback réception audio optionnel |

## Corrections appliquées

| Problème | Solution |
|----------|----------|
| Bouton vidéo invisible en VIDEO | `setupInitialStates()` dans `setCallType()` uniquement |
| Webcam ne marche pas | `webcam-capture` (Sarxos) + `SwingFXUtils` |
| Audio ne marche pas | Fallback micro 8000 Hz → 44100 Hz ; relais UDP après `MEETING_INFO` |
| Bouton ajout participant | `fx:id="btnAddParticipant"` + `requestNonMeetingParticipants` |
| Menu « Minimiser » | Supprimé du menu ⋯ |
| Participant quitte → reste affiché | `removeParticipantByUsername(msg.getFrom())` |
| Avatar au lieu webcam | `createParticipantVideoContainer()` avec `ImageView` |

## Flux

1. `openMeetingWindow()` → `setCallType()` → `startServices()` (audio + vidéo si VIDEO).
2. `MEETING_INFO` → `configureUdpConnection()` → `startMeetingAudio/Video` + `handoffToRelay()`.
3. Fermeture → `cleanup()` + `endMeeting` / `leaveMeeting`.

## Test rapide

1. Démarrer un appel **AUDIO** : pas de bouton vidéo, tuiles participants au centre.
2. Démarrer un appel **VIDEO** : bouton vidéo gris visible, grille 2 colonnes, webcam locale.
3. Inviter un membre via **👤+**.
4. Un participant quitte : sa tuile disparaît.

## Note `getUserId()`

`ChatMessage` n’a pas de `getUserId()`. Le retrait utilise `msg.getFrom()` (nom d’utilisateur) via `removeParticipantByUsername()`.
