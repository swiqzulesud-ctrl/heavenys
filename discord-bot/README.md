# Bot Discord — Parties personnalisées + Welcome/Goodbye

Bot Discord pour une communauté Valorant : **parties personnalisées** et système
**Welcome & Goodbye** automatisé.

## Fonctionnalités

### Parties personnalisées
- Création réservée au rôle **Organisateur de Parties** (`/partie creer`)
- Embed + répartition alternée 5v5 + salons vocaux (`Game A/B/C` si multi)
- Boutons 🔊 vocal / 🎮 infos (lien ou éphémère)
- `/resultat` → Modal score + MVP · cleanup vocal `delete` | `archive`

### Welcome & Goodbye
Un **seul salon** annonce arrivées et départs via des embeds anglais, avec avatar,
bannière, compteur de membres, logo serveur et boutons de navigation :

| Bouton | Rôle |
|---|---|
| 📜 Rules | Lien vers le salon des règles |
| 🎭 Roles | Lien vers l'attribution des rôles |
| 💬 General Chat | Salon de discussion principal |
| 🌐 Website | Optionnel — site / Discord communautaire |

Toute la personnalisation (salon, couleurs, bannières, textes, emojis, boutons, liens)
se fait dans **`config/welcome.json`** — aucun changement de code requis.

## Prérequis Discord

1. Application + bot sur le [Developer Portal](https://discord.com/developers/applications)
2. Privileged Intent : **Server Members Intent** (obligatoire pour welcome/goodbye)
3. Permissions : Gérer les salons, Envoyer des messages, Intégrer des liens, Déplacer les membres (optionnel), Commandes slash
4. Rôle `Organisateur de Parties` (parties)
5. Salons : parties, welcome, règles, rôles, général + catégorie vocale

## Installation

```bash
cd discord-bot
cp .env.example .env
cp config/welcome.example.json config/welcome.json
# Éditer .env et config/welcome.json

npm install
npm run register
npm start
```

## Configuration Welcome (`config/welcome.json`)

```bash
cp config/welcome.example.json config/welcome.json
```

Champs principaux :

| Champ | Description |
|---|---|
| `enabled` | Active / désactive le module |
| `channelId` | Salon unique pour welcome **et** goodbye |
| `colors.welcome` / `colors.goodbye` | Couleurs hex (`#5865F2`) |
| `images.*Banner` / `serverLogo` | URLs d'images (vide = icône du serveur) |
| `welcome` / `goodbye` | Titre, description, footer, toggles avatar/bannière/compteur |
| `buttons[]` | Labels, emojis, `channelId` et/ou `url`, `enabled` |

Placeholders disponibles dans les textes : `{member}` `{username}` `{display}`
`{tag}` `{id}` `{server}` `{count}` `{joinedAt}` `{createdAt}`.

Après édition : `/welcome reload` (ou redémarrage du bot).

## Variables d'environnement

| Variable | Description |
|---|---|
| `DISCORD_TOKEN` | Token du bot |
| `CLIENT_ID` | Application ID |
| `GUILD_ID` | ID du serveur |
| `GAMES_CHANNEL_ID` | Salon des embeds de parties |
| `VOICE_CATEGORY_ID` | Catégorie des salons d'équipe |
| `ARCHIVE_CATEGORY_ID` | Archives vocales (mode `archive`) |
| `VOICE_CLEANUP_MODE` | `delete` ou `archive` |
| `ORGANIZER_ROLE_NAME` | Défaut : `Organisateur de Parties` |
| `TEAM_SIZE` | Joueurs par équipe (défaut `5`) |

## Commandes

| Commande | Description |
|---|---|
| `/partie creer [lobby] [code] [invitation]` | Lance une partie + vocaux |
| `/partie infos [id] …` | Met à jour lobby / code / lien |
| `/partie liste` | Liste les parties actives |
| `/partie annuler [id]` | Annule une partie |
| `/resultat [id]` | Modal score + MVP |
| `/welcome status` | État du module welcome |
| `/welcome reload` | Recharge `config/welcome.json` |
| `/welcome preview` | Aperçu éphémère welcome/goodbye |

## Architecture

```
discord-bot/
├── config/
│   └── welcome.example.json     # Modèle (copier vers welcome.json)
├── src/
│   ├── index.js
│   ├── register-commands.js
│   ├── config.js                # .env (parties)
│   ├── commands/
│   │   ├── partie.js
│   │   ├── resultat.js
│   │   └── welcome.js
│   ├── services/
│   │   ├── GameManager.js
│   │   └── GameStore.js
│   ├── welcome/
│   │   ├── WelcomeService.js    # GuildMemberAdd / Remove
│   │   ├── configLoader.js      # JSON hot-reloadable
│   │   └── embeds.js
│   └── utils/
├── .env.example
└── package.json
```
