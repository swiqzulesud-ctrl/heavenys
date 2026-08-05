# Bot Discord — Parties personnalisées Valorant

Bot Discord qui organise les **parties personnalisées** (custom games) pour une communauté
Valorant : création réservée aux organisateurs, répartition automatique des joueurs, salons
vocaux d'équipe, et clôture via un formulaire (score + MVP).

## Fonctionnalités

### Création (`/partie creer`)
- Réservée au rôle **Organisateur de Parties** (configurable).
- Publie un embed dans le salon dédié : Équipe 1 / Équipe 2, liste des joueurs, organisateur
  en pied de page.
- Crée automatiquement deux salons vocaux :
  - Une seule partie : `🎧 Équipe 1` / `🎧 Équipe 2`
  - Plusieurs parties en parallèle : `🎧 Équipe 1 - Game A`, `🎧 Équipe 2 - Game A`, etc.

### Inscription
Boutons sur l'embed : **Rejoindre** / **Quitter** / **Annuler**.

Répartition **alternée** à l'inscription :
1er joueur → Équipe 1, 2e → Équipe 2, 3e → Équipe 1, … jusqu'à `TEAM_SIZE` par équipe
(défaut **5**, règles compétitives Valorant). En cas d'égalité réglementaire, l'OT
continue jusqu'à **+2 manches** (rappelé dans l'embed ; les scores à égalité sont refusés).

### Accès rapide (sous l'embed)
Deux boutons supplémentaires :

| Bouton | Comportement |
|---|---|
| 🔊 **Rejoindre le vocal** | Identifie l'équipe du joueur, le déplace si possible, et répond en **éphémère** avec un **bouton URL** `https://discord.com/channels/ID_SERVEUR/ID_SALON_VOCAL` vers le vocal de son équipe. |
| 🎮 **Informations de la partie** | Si un `invitation` (URL) a été fourni → **bouton lien** direct. Sinon → message **éphémère** avec lobby / code (privés). |

Renseigner les infos à la création (`/partie creer lobby:… code:… invitation:…`) ou plus tard via `/partie infos`.

### Résultat (`/resultat`)
Ouvre un **Modal Discord** pour saisir :
- Score Équipe 1 / Équipe 2
- Pseudo du MVP + K/D/A (`24/12/5`)

Après validation : l'embed est mis à jour (score final + MVP), la partie est marquée
terminée, et les vocaux sont **supprimés** ou **archivés** selon `VOICE_CLEANUP_MODE`.

Aucune statistique individuelle hors MVP n'est stockée.

## Prérequis Discord

1. Créer une application + bot sur le [Developer Portal](https://discord.com/developers/applications).
2. Privileged Gateway Intent : **Server Members Intent** (recommandé pour le rôle).
3. Inviter le bot avec les permissions :
   - Gérer les salons
   - Voir les salons / Envoyer des messages / Intégrer des liens
   - Déplacer les membres (optionnel, pour auto-move vocal)
   - Utiliser les commandes slash
4. Créer le rôle nommé exactement `Organisateur de Parties` (ou adapter `.env`).
5. Préparer : salon texte des parties, catégorie vocale, (optionnel) catégorie d'archives.

## Installation

```bash
cd discord-bot
cp .env.example .env
# Éditer .env avec le token, les IDs, etc.

npm install
npm run register   # enregistre /partie et /resultat sur le serveur
npm start
```

## Variables d'environnement

| Variable | Description |
|---|---|
| `DISCORD_TOKEN` | Token du bot |
| `CLIENT_ID` | Application ID |
| `GUILD_ID` | ID du serveur |
| `GAMES_CHANNEL_ID` | Salon où poster les embeds |
| `VOICE_CATEGORY_ID` | Catégorie des salons d'équipe |
| `ARCHIVE_CATEGORY_ID` | Catégorie d'archives (si mode `archive`) |
| `VOICE_CLEANUP_MODE` | `delete` ou `archive` |
| `ORGANIZER_ROLE_NAME` | Défaut : `Organisateur de Parties` |
| `TEAM_SIZE` | Joueurs par équipe (défaut `5`) |

## Commandes

| Commande | Description |
|---|---|
| `/partie creer [lobby] [code] [invitation]` | Lance une partie + vocaux (+ infos connexion) |
| `/partie infos [id] …` | Met à jour lobby / code / lien d'invitation |
| `/partie liste` | Liste les parties actives |
| `/partie annuler [id]` | Annule une partie |
| `/resultat [id]` | Modal score + MVP |

L'`id` est le préfixe affiché en bas de l'embed (8 caractères) ou `Game A` / `Game B`…
S'il n'y a qu'une partie en cours, l'`id` est optionnel.

## Architecture

```
discord-bot/
├── src/
│   ├── index.js                 # Client Discord + handlers
│   ├── register-commands.js     # Déploiement des slash commands
│   ├── config.js
│   ├── commands/
│   │   ├── partie.js
│   │   └── resultat.js
│   ├── services/
│   │   ├── GameManager.js       # Création, join, vocaux, résultat
│   │   └── GameStore.js         # Persistance JSON (data/games.json)
│   └── utils/
│       ├── embeds.js
│       └── score.js
├── .env.example
└── package.json
```

Les parties actives sont persistées dans `data/games.json` pour survivre à un redémarrage
du processus (les boutons restent valides tant que le message existe).
