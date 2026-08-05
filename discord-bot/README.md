# Bot Discord — Parties personnalisées + Welcome/Goodbye

Bot Discord pour une communauté Valorant : **parties personnalisées** et système
**Welcome & Goodbye** automatisé, configurable entièrement via commandes slash.

## Welcome & Goodbye

Un **seul salon** annonce arrivées et départs. Persisté dans `data/welcome.json`
(base JSON, modifiable sans toucher au code).

### Contenu des embeds
- Avatar (GIF animé supporté) · bannière HD · logo serveur
- Nom du serveur / membre · compteur live · date d'arrivée ou de départ
- Détection des comptes récents (`newAccountDays`)
- Couleurs, textes et footer personnalisables
- Boutons Link : 📜 Rules · 🎭 Roles · 💬 General · 🌐 Website · 🎮 Valorant Tracker

### Commandes admin (`Manage Guild`)

| Commande | Description |
|---|---|
| `/welcome setup` | Configure salon, bannière, couleur, footer, logo et boutons |
| `/welcome channel` | Définit le salon d'annonce |
| `/welcome banner` | Change la bannière (background) |
| `/welcome color` | Couleur hex (`#FF4655`) — welcome / goodbye / both |
| `/welcome message` | Texte de bienvenue (`{member}`, `{server}`, …) |
| `/goodbye message` | Texte de départ |
| `/welcome footer` | Footer des embeds |
| `/welcome logo` | Logo (sinon icône du serveur) |
| `/welcome preview` | Aperçu éphémère |
| `/welcome test` | Envoie un faux message dans le salon configuré |
| `/welcome enable` / `disable` | Active / coupe le système |
| `/welcome status` / `reload` | État & rechargement du fichier |

Exemple rapide :

```
/welcome setup channel:#welcome rules:#rules roles:#roles general:#general
  banner:https://… color:#FF4655 tracker:https://tracker.gg/valorant
```

## Parties personnalisées

Création (`/partie creer`), répartition alternée 5v5, vocaux auto (`Game A/B/C`),
boutons 🔊 / 🎮, résultat via Modal (`/resultat`).

## Installation

```bash
cd discord-bot
cp .env.example .env
# optionnel : cp config/welcome.example.json → seed via premier démarrage auto
npm install
npm run register
npm start
```

Intent requis : **Server Members Intent**.

Puis `/welcome setup` sur le serveur.

## Variables d'environnement (parties)

| Variable | Description |
|---|---|
| `DISCORD_TOKEN` / `CLIENT_ID` / `GUILD_ID` | Bot |
| `GAMES_CHANNEL_ID` / `VOICE_CATEGORY_ID` | Parties |
| `ARCHIVE_CATEGORY_ID` / `VOICE_CLEANUP_MODE` | Cleanup vocal |
| `ORGANIZER_ROLE_NAME` / `TEAM_SIZE` | Org + taille d'équipe |

## Architecture

```
discord-bot/
├── config/welcome.example.json   # modèle
├── data/welcome.json             # DB runtime (gitignore)
├── src/
│   ├── welcome/
│   │   ├── WelcomeService.js     # events + mutations
│   │   ├── configLoader.js       # load/save JSON DB
│   │   └── embeds.js
│   ├── commands/
│   │   ├── welcome.js
│   │   ├── goodbye.js
│   │   ├── partie.js
│   │   └── resultat.js
│   └── …
└── package.json
```
