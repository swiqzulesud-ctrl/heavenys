'use strict';

const {
  EmbedBuilder,
  ActionRowBuilder,
  ButtonBuilder,
  ButtonStyle,
  Colors,
} = require('discord.js');

const GAME_LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';

function gameLabel(slotIndex) {
  if (slotIndex < 0) return null;
  if (slotIndex < GAME_LETTERS.length) return `Game ${GAME_LETTERS[slotIndex]}`;
  return `Game ${slotIndex + 1}`;
}

function formatPlayerList(players) {
  if (!players.length) return '_Aucun joueur_';
  return players.map((p, i) => `\`${i + 1}.\` <@${p.id}>`).join('\n');
}

/**
 * Build the public match embed (waiting / in progress / finished).
 */
function buildGameEmbed(game, { multi = false } = {}) {
  const label = multi && game.slotIndex != null ? gameLabel(game.slotIndex) : null;
  const title = label ? `Partie personnalisée — ${label}` : 'Partie personnalisée';

  const team1Count = game.team1.length;
  const team2Count = game.team2.length;
  const max = game.teamSize;

  let description =
    'Répartition automatique en alternance (1 → Équipe 1, 2 → Équipe 2, …).\n' +
    'Règles : **Valorant Compétitif** — en cas d\'égalité, prolongation (OT) jusqu\'à **+2 manches**.';

  if (game.status === 'finished' && game.result) {
    const { score1, score2, mvp } = game.result;
    const winner = score1 > score2 ? 'Équipe 1' : 'Équipe 2';
    description =
      `**Partie terminée** — Victoire : **${winner}**\n` +
      `Score final : **${score1}** – **${score2}**`;
    if (mvp) {
      description += `\n\n⭐ **MVP** : **${mvp.name}** — K/D/A \`${mvp.kills}/${mvp.deaths}/${mvp.assists}\``;
    }
  } else if (game.status === 'cancelled') {
    description = '_Cette partie a été annulée._';
  } else if (team1Count >= max && team2Count >= max) {
    description += '\n\n✅ **Équipes complètes** — la rencontre peut commencer.';
  }

  const color =
    game.status === 'finished'
      ? Colors.Green
      : game.status === 'cancelled'
        ? Colors.DarkGrey
        : team1Count >= max && team2Count >= max
          ? Colors.Gold
          : Colors.Blurple;

  return new EmbedBuilder()
    .setColor(color)
    .setTitle(title)
    .setDescription(description)
    .addFields(
      {
        name: `🔵 Équipe 1 (${team1Count}/${max})`,
        value: formatPlayerList(game.team1),
        inline: true,
      },
      {
        name: `🔴 Équipe 2 (${team2Count}/${max})`,
        value: formatPlayerList(game.team2),
        inline: true,
      },
    )
    .setFooter({
      text: `Organisateur : ${game.organizerTag} · ID ${game.id.slice(0, 8)}`,
    })
    .setTimestamp(game.createdAt ? new Date(game.createdAt) : new Date());
}

function buildJoinButtons(gameId, disabled = false) {
  return new ActionRowBuilder().addComponents(
    new ButtonBuilder()
      .setCustomId(`game:join:${gameId}`)
      .setLabel('Rejoindre')
      .setStyle(ButtonStyle.Success)
      .setDisabled(disabled),
    new ButtonBuilder()
      .setCustomId(`game:leave:${gameId}`)
      .setLabel('Quitter')
      .setStyle(ButtonStyle.Secondary)
      .setDisabled(disabled),
    new ButtonBuilder()
      .setCustomId(`game:cancel:${gameId}`)
      .setLabel('Annuler')
      .setStyle(ButtonStyle.Danger)
      .setDisabled(disabled),
  );
}

function voiceChannelName(teamNumber, { multi = false, slotIndex = 0 } = {}) {
  const base = `🎧 Équipe ${teamNumber}`;
  if (!multi) return base;
  return `${base} - ${gameLabel(slotIndex)}`;
}

module.exports = {
  GAME_LETTERS,
  gameLabel,
  buildGameEmbed,
  buildJoinButtons,
  voiceChannelName,
};
