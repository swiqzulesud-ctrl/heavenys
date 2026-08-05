'use strict';

const { SlashCommandBuilder } = require('discord.js');

module.exports = {
  data: new SlashCommandBuilder()
    .setName('partie')
    .setDescription('Gestion des parties personnalisées Valorant')
    .addSubcommand((sub) =>
      sub
        .setName('creer')
        .setDescription(
          'Créer une partie (rôle Organisateur de Parties). Embed + salons vocaux automatiques.',
        )
        .addStringOption((opt) =>
          opt
            .setName('lobby')
            .setDescription('Nom du lobby Valorant (affiché en éphémère via 🎮)')
            .setRequired(false),
        )
        .addStringOption((opt) =>
          opt
            .setName('code')
            .setDescription('Code / mot de passe du lobby (éphémère via 🎮)')
            .setRequired(false),
        )
        .addStringOption((opt) =>
          opt
            .setName('invitation')
            .setDescription('Lien d\'invitation (Riot ou autre) — le bouton 🎮 devient un lien URL')
            .setRequired(false),
        ),
    )
    .addSubcommand((sub) =>
      sub
        .setName('infos')
        .setDescription('Mettre à jour les infos de connexion (lobby / code / lien)')
        .addStringOption((opt) =>
          opt
            .setName('id')
            .setDescription('Identifiant court ou Game A/B/…')
            .setRequired(false),
        )
        .addStringOption((opt) =>
          opt.setName('lobby').setDescription('Nom du lobby').setRequired(false),
        )
        .addStringOption((opt) =>
          opt.setName('code').setDescription('Code / mot de passe').setRequired(false),
        )
        .addStringOption((opt) =>
          opt
            .setName('invitation')
            .setDescription('Lien d\'invitation (bouton 🎮 en mode URL)')
            .setRequired(false),
        )
        .addBooleanOption((opt) =>
          opt
            .setName('retirer_invitation')
            .setDescription('Retirer le lien URL et repasser le bouton 🎮 en mode éphémère')
            .setRequired(false),
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

    if (sub === 'infos') {
      return gameManager.updateGameInfo(interaction);
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
