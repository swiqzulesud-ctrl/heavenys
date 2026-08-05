'use strict';

const {
  SlashCommandBuilder,
  ModalBuilder,
  TextInputBuilder,
  TextInputStyle,
  ActionRowBuilder,
} = require('discord.js');

module.exports = {
  data: new SlashCommandBuilder()
    .setName('partie')
    .setDescription('Gestion des parties personnalisées Valorant')
    .addSubcommand((sub) =>
      sub
        .setName('creer')
        .setDescription(
          'Créer une partie (rôle Organisateur de Parties). Embed + salons vocaux automatiques.',
        ),
    )
    .addSubcommand((sub) =>
      sub
        .setName('liste')
        .setDescription('Lister les parties en cours'),
    )
    .addSubcommand((sub) =>
      sub
        .setName('annuler')
        .setDescription('Annuler une partie en cours')
        .addStringOption((opt) =>
          opt
            .setName('id')
            .setDescription('Identifiant court de la partie (bas de l\'embed) ou Game A/B/…')
            .setRequired(false),
        ),
    ),

  /**
   * @param {import('discord.js').ChatInputCommandInteraction} interaction
   * @param {{ gameManager: import('../services/GameManager').GameManager }} ctx
   */
  async execute(interaction, ctx) {
    const sub = interaction.options.getSubcommand();
    const { gameManager } = ctx;

    if (sub === 'creer') {
      return gameManager.createGame(interaction);
    }

    if (sub === 'liste') {
      const active = gameManager.store.listActive();
      if (!active.length) {
        return interaction.reply({ content: 'Aucune partie en cours.', ephemeral: true });
      }
      const { gameLabel } = require('../utils/embeds');
      const multi = active.length > 1;
      const lines = active.map((g) => {
        const label = multi ? gameLabel(g.slotIndex) : 'Partie unique';
        const filled = `${g.team1.length + g.team2.length}/${g.teamSize * 2}`;
        return `• **${label}** — \`${g.id.slice(0, 8)}\` — ${filled} joueurs — org. <@${g.organizerId}>`;
      });
      return interaction.reply({
        content: `**Parties en cours** (${active.length})\n${lines.join('\n')}`,
        ephemeral: true,
      });
    }

    if (sub === 'annuler') {
      const query = interaction.options.getString('id');
      const { game, error } = gameManager.resolveGame(query);
      if (error) {
        return interaction.reply({ content: error, ephemeral: true });
      }
      return gameManager.cancelGame(interaction, game.id);
    }
  },
};
