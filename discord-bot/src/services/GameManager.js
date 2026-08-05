'use strict';

const { ChannelType, PermissionFlagsBits } = require('discord.js');
const { randomUUID } = require('node:crypto');
const {
  buildGameEmbed,
  buildJoinButtons,
  voiceChannelName,
} = require('../utils/embeds');

class GameManager {
  /**
   * @param {import('discord.js').Client} client
   * @param {import('./GameStore').GameStore} store
   * @param {object} config
   */
  constructor(client, store, config) {
    this.client = client;
    this.store = store;
    this.config = config;
  }

  isOrganizer(member) {
    if (!member) return false;
    if (member.permissions?.has(PermissionFlagsBits.Administrator)) return true;
    return member.roles.cache.some((r) => r.name === this.config.organizerRoleName);
  }

  findPlayerGame(userId) {
    return this.store.listActive().find(
      (g) =>
        g.team1.some((p) => p.id === userId) || g.team2.some((p) => p.id === userId),
    );
  }

  /** Recompute Game A/B/C labels and rename voice channels when multi-match. */
  async refreshLabels(guild) {
    const active = this.store.listActive().sort((a, b) => a.createdAt - b.createdAt);
    const multi = active.length > 1;

    for (let i = 0; i < active.length; i++) {
      const game = active[i];
      const prevSlot = game.slotIndex;
      const prevMulti = game.multi === true;
      game.slotIndex = i;
      game.multi = multi;

      if (prevSlot !== i || prevMulti !== multi) {
        this.store.upsert(game);
        await this._renameVoiceChannels(guild, game, multi);
        await this._updateMessage(guild, game);
      }
    }
  }

  async _renameVoiceChannels(guild, game, multi) {
    for (const [teamKey, teamNum] of [
      ['voiceChannel1Id', 1],
      ['voiceChannel2Id', 2],
    ]) {
      const channelId = game[teamKey];
      if (!channelId) continue;
      const channel = await guild.channels.fetch(channelId).catch(() => null);
      if (!channel) continue;
      const name = voiceChannelName(teamNum, { multi, slotIndex: game.slotIndex });
      if (channel.name !== name) {
        await channel.setName(name).catch((err) => {
          console.warn(`[GameManager] Rename vocal échoué (${name}):`, err.message);
        });
      }
    }
  }

  async _updateMessage(guild, game) {
    if (!game.channelId || !game.messageId) return;
    const channel = await guild.channels.fetch(game.channelId).catch(() => null);
    if (!channel?.isTextBased()) return;
    const message = await channel.messages.fetch(game.messageId).catch(() => null);
    if (!message) return;

    const finished = game.status === 'finished' || game.status === 'cancelled';
    const multi = !finished && this.store.listActive().length > 1;

    await message.edit({
      embeds: [buildGameEmbed(game, { multi })],
      components: finished ? [] : [buildJoinButtons(game.id, false)],
    });
  }

  async createGame(interaction) {
    const guild = interaction.guild;
    if (!guild) {
      return interaction.reply({ content: 'Cette commande doit être utilisée sur un serveur.', ephemeral: true });
    }

    if (!this.isOrganizer(interaction.member)) {
      return interaction.reply({
        content: `Seul le rôle **${this.config.organizerRoleName}** peut créer une partie.`,
        ephemeral: true,
      });
    }

    await interaction.deferReply({ ephemeral: true });

    const gamesChannel = await guild.channels.fetch(this.config.gamesChannelId).catch(() => null);
    if (!gamesChannel?.isTextBased()) {
      return interaction.editReply({
        content: 'Salon des parties introuvable. Vérifiez `GAMES_CHANNEL_ID`.',
      });
    }

    const voiceCategory = await guild.channels.fetch(this.config.voiceCategoryId).catch(() => null);
    if (!voiceCategory || voiceCategory.type !== ChannelType.GuildCategory) {
      return interaction.editReply({
        content: 'Catégorie vocale introuvable. Vérifiez `VOICE_CATEGORY_ID`.',
      });
    }

    const activeBefore = this.store.listActive();
    const slotIndex = activeBefore.length;
    // Second+ concurrent game → all matches use Game A/B/C labels.
    const willBeMulti = activeBefore.length >= 1;

    const id = randomUUID();
    const game = {
      id,
      status: 'open',
      teamSize: this.config.teamSize,
      team1: [],
      team2: [],
      joinOrder: [],
      organizerId: interaction.user.id,
      organizerTag: interaction.user.tag,
      slotIndex,
      multi: willBeMulti,
      voiceChannel1Id: null,
      voiceChannel2Id: null,
      channelId: gamesChannel.id,
      messageId: null,
      createdAt: Date.now(),
      result: null,
    };

    try {
      const vc1 = await guild.channels.create({
        name: voiceChannelName(1, { multi: willBeMulti, slotIndex }),
        type: ChannelType.GuildVoice,
        parent: voiceCategory.id,
        reason: `Partie personnalisée ${id.slice(0, 8)} — Équipe 1`,
      });
      const vc2 = await guild.channels.create({
        name: voiceChannelName(2, { multi: willBeMulti, slotIndex }),
        type: ChannelType.GuildVoice,
        parent: voiceCategory.id,
        reason: `Partie personnalisée ${id.slice(0, 8)} — Équipe 2`,
      });
      game.voiceChannel1Id = vc1.id;
      game.voiceChannel2Id = vc2.id;
    } catch (err) {
      console.error('[GameManager] Création vocaux échouée:', err);
      return interaction.editReply({
        content: `Impossible de créer les salons vocaux : ${err.message}`,
      });
    }

    this.store.upsert(game);

    // Refresh labels on all active games (including the new one)
    await this.refreshLabels(guild);

    const refreshed = this.store.get(id);
    const multiNow = this.store.listActive().length > 1;

    const message = await gamesChannel.send({
      embeds: [buildGameEmbed(refreshed, { multi: multiNow })],
      components: [buildJoinButtons(id)],
    });

    refreshed.messageId = message.id;
    this.store.upsert(refreshed);

    return interaction.editReply({
      content: `Partie créée dans ${gamesChannel}${multiNow ? ` (**${require('../utils/embeds').gameLabel(refreshed.slotIndex)}**)` : ''}.`,
    });
  }

  async joinGame(interaction, gameId) {
    const game = this.store.get(gameId);
    if (!game || game.status === 'finished' || game.status === 'cancelled') {
      return interaction.reply({ content: 'Cette partie n\'est plus disponible.', ephemeral: true });
    }

    const userId = interaction.user.id;
    if (game.team1.some((p) => p.id === userId) || game.team2.some((p) => p.id === userId)) {
      return interaction.reply({ content: 'Vous êtes déjà dans cette partie.', ephemeral: true });
    }

    const other = this.findPlayerGame(userId);
    if (other && other.id !== game.id) {
      return interaction.reply({
        content: 'Vous participez déjà à une autre partie en cours.',
        ephemeral: true,
      });
    }

    if (game.team1.length >= game.teamSize && game.team2.length >= game.teamSize) {
      return interaction.reply({ content: 'Les deux équipes sont déjà complètes.', ephemeral: true });
    }

    // Alternating assignment by join order: 0→T1, 1→T2, 2→T1, ...
    // Skip a full team if needed.
    const nextIndex = game.joinOrder.length;
    let team = nextIndex % 2 === 0 ? 1 : 2;
    if (team === 1 && game.team1.length >= game.teamSize) team = 2;
    if (team === 2 && game.team2.length >= game.teamSize) team = 1;
    if (team === 1 && game.team1.length >= game.teamSize) {
      return interaction.reply({ content: 'Plus de place disponible.', ephemeral: true });
    }

    const player = {
      id: userId,
      tag: interaction.user.tag,
      joinedAt: Date.now(),
    };

    if (team === 1) game.team1.push(player);
    else game.team2.push(player);
    game.joinOrder.push(userId);

    if (game.team1.length >= game.teamSize && game.team2.length >= game.teamSize) {
      game.status = 'ready';
    }

    this.store.upsert(game);
    await this._updateMessage(interaction.guild, game);

    // Move to team voice if already connected somewhere
    const voiceId = team === 1 ? game.voiceChannel1Id : game.voiceChannel2Id;
    const member = interaction.member;
    if (member?.voice?.channelId && voiceId) {
      await member.voice.setChannel(voiceId).catch(() => null);
    }

    return interaction.reply({
      content: `Vous avez rejoint l'**Équipe ${team}**.`,
      ephemeral: true,
    });
  }

  async leaveGame(interaction, gameId) {
    const game = this.store.get(gameId);
    if (!game || game.status === 'finished' || game.status === 'cancelled') {
      return interaction.reply({ content: 'Cette partie n\'est plus disponible.', ephemeral: true });
    }

    const userId = interaction.user.id;
    const in1 = game.team1.findIndex((p) => p.id === userId);
    const in2 = game.team2.findIndex((p) => p.id === userId);
    if (in1 < 0 && in2 < 0) {
      return interaction.reply({ content: 'Vous n\'êtes pas dans cette partie.', ephemeral: true });
    }

    if (in1 >= 0) game.team1.splice(in1, 1);
    if (in2 >= 0) game.team2.splice(in2, 1);
    game.joinOrder = game.joinOrder.filter((id) => id !== userId);
    if (game.status === 'ready') game.status = 'open';

    this.store.upsert(game);
    await this._updateMessage(interaction.guild, game);

    return interaction.reply({ content: 'Vous avez quitté la partie.', ephemeral: true });
  }

  async cancelGame(interaction, gameId) {
    const game = this.store.get(gameId);
    if (!game || game.status === 'finished' || game.status === 'cancelled') {
      return interaction.reply({ content: 'Cette partie n\'est plus disponible.', ephemeral: true });
    }

    if (!this.isOrganizer(interaction.member) && interaction.user.id !== game.organizerId) {
      return interaction.reply({
        content: 'Seul l\'organisateur ou un membre du rôle organisateur peut annuler.',
        ephemeral: true,
      });
    }

    await interaction.deferReply({ ephemeral: true });

    game.status = 'cancelled';
    this.store.upsert(game);
    await this._cleanupVoice(interaction.guild, game);
    await this._updateMessage(interaction.guild, game);
    await this.refreshLabels(interaction.guild);

    return interaction.editReply({ content: 'Partie annulée. Salons vocaux nettoyés.' });
  }

  /**
   * Open the result modal context — validation happens in modal submit.
   * Caller must show the modal; this only checks permissions / game state.
   */
  assertCanSubmitResult(member, userId, game) {
    if (!game || game.status === 'finished' || game.status === 'cancelled') {
      return 'Cette partie n\'est plus disponible.';
    }
    if (!this.isOrganizer(member) && userId !== game.organizerId) {
      return `Seul l'organisateur ou le rôle **${this.config.organizerRoleName}** peut enregistrer le résultat.`;
    }
    return null;
  }

  async finishGame(guild, game, result) {
    game.status = 'finished';
    game.result = result;
    game.finishedAt = Date.now();
    this.store.upsert(game);

    await this._updateMessage(guild, game);
    await this._cleanupVoice(guild, game);
    await this.refreshLabels(guild);
  }

  async _cleanupVoice(guild, game) {
    const mode = this.config.voiceCleanupMode;
    const channels = [game.voiceChannel1Id, game.voiceChannel2Id].filter(Boolean);

    if (mode === 'archive') {
      const archiveId = this.config.archiveCategoryId;
      if (!archiveId) {
        console.warn('[GameManager] ARCHIVE_CATEGORY_ID manquant — suppression des vocaux.');
        for (const id of channels) {
          const ch = await guild.channels.fetch(id).catch(() => null);
          if (ch) await ch.delete('Partie terminée (archive indisponible)').catch(() => null);
        }
        return;
      }
      for (const id of channels) {
        const ch = await guild.channels.fetch(id).catch(() => null);
        if (!ch) continue;
        await ch.setParent(archiveId, { lockPermissions: false }).catch((err) => {
          console.warn('[GameManager] Archive vocal échouée:', err.message);
        });
        const archivedName = ch.name.startsWith('📦 ') ? ch.name : `📦 ${ch.name}`;
        if (ch.name !== archivedName) {
          await ch.setName(archivedName).catch(() => null);
        }
      }
      return;
    }

    for (const id of channels) {
      const ch = await guild.channels.fetch(id).catch(() => null);
      if (ch) await ch.delete('Partie personnalisée terminée').catch(() => null);
    }
  }

  /** Resolve a game by short id prefix, full id, or the only active game. */
  resolveGame(query) {
    const active = this.store.listActive();
    if (!query) {
      if (active.length === 1) return { game: active[0] };
      if (active.length === 0) return { error: 'Aucune partie en cours.' };
      return {
        error:
          'Plusieurs parties sont en cours. Précisez l\'identifiant (visible en bas de l\'embed).',
      };
    }
    const q = String(query).trim().toLowerCase();
    const match =
      this.store.get(q) ||
      [...this.store.games.values()].find((g) => g.id.toLowerCase().startsWith(q)) ||
      active.find((g) => {
        const { gameLabel } = require('../utils/embeds');
        return gameLabel(g.slotIndex).toLowerCase() === q || `game ${String.fromCharCode(65 + g.slotIndex)}`.toLowerCase() === q;
      });

    if (!match) return { error: `Partie introuvable : \`${query}\`.` };
    if (match.status === 'finished' || match.status === 'cancelled') {
      return { error: 'Cette partie est déjà terminée ou annulée.' };
    }
    return { game: match };
  }
}

module.exports = { GameManager };
