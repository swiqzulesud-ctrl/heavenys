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
    .setName('resultat')
    .setDescription('Enregistrer le résultat d\'une partie (score + MVP)')
    .addStringOption((opt) =>
      opt
        .setName('id')
        .setDescription('Identifiant court (bas de l\'embed) ou Game A/B/… — optionnel s\'il n\'y en a qu\'une')
        .setRequired(false),
    ),

  /**
   * @param {import('discord.js').ChatInputCommandInteraction} interaction
   * @param {{ gameManager: import('../services/GameManager').GameManager }} ctx
   */
  async execute(interaction, ctx) {
    const { gameManager } = ctx;
    const query = interaction.options.getString('id');
    const { game, error } = gameManager.resolveGame(query);
    if (error) {
      return interaction.reply({ content: error, ephemeral: true });
    }

    const denied = gameManager.assertCanSubmitResult(
      interaction.member,
      interaction.user.id,
      game,
    );
    if (denied) {
      return interaction.reply({ content: denied, ephemeral: true });
    }

    const modal = new ModalBuilder()
      .setCustomId(`game:result:${game.id}`)
      .setTitle('Résultat de la partie');

    const score1 = new TextInputBuilder()
      .setCustomId('score1')
      .setLabel('Score Équipe 1')
      .setStyle(TextInputStyle.Short)
      .setPlaceholder('ex. 13')
      .setRequired(true)
      .setMaxLength(3);

    const score2 = new TextInputBuilder()
      .setCustomId('score2')
      .setLabel('Score Équipe 2')
      .setStyle(TextInputStyle.Short)
      .setPlaceholder('ex. 11')
      .setRequired(true)
      .setMaxLength(3);

    const mvpName = new TextInputBuilder()
      .setCustomId('mvp_name')
      .setLabel('Pseudo du MVP')
      .setStyle(TextInputStyle.Short)
      .setPlaceholder('Pseudo in-game')
      .setRequired(true)
      .setMaxLength(32);

    const mvpKda = new TextInputBuilder()
      .setCustomId('mvp_kda')
      .setLabel('K / D / A du MVP')
      .setStyle(TextInputStyle.Short)
      .setPlaceholder('ex. 24/12/5')
      .setRequired(true)
      .setMaxLength(16);

    modal.addComponents(
      new ActionRowBuilder().addComponents(score1),
      new ActionRowBuilder().addComponents(score2),
      new ActionRowBuilder().addComponents(mvpName),
      new ActionRowBuilder().addComponents(mvpKda),
    );

    await interaction.showModal(modal);
  },
};
