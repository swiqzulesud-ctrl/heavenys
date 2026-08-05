'use strict';

const { SlashCommandBuilder, PermissionFlagsBits } = require('discord.js');

module.exports = {
  data: new SlashCommandBuilder()
    .setName('goodbye')
    .setDescription('Goodbye message customization')
    .setDefaultMemberPermissions(PermissionFlagsBits.ManageGuild)
    .addSubcommand((sub) =>
      sub
        .setName('message')
        .setDescription('Edit the goodbye message text')
        .addStringOption((opt) =>
          opt
            .setName('text')
            .setDescription('New goodbye description (supports {member}, {server}, …)')
            .setRequired(true),
        ),
    ),

  async execute(interaction, ctx) {
    const { welcomeService } = ctx;
    if (!welcomeService) {
      return interaction.reply({ content: 'Welcome service unavailable.', ephemeral: true });
    }

    const sub = interaction.options.getSubcommand();
    if (sub === 'message') {
      const text = interaction.options.getString('text', true);
      welcomeService.setGoodbyeMessage(text);
      return interaction.reply({
        content: 'Goodbye message updated. Placeholders: `{member}` `{server}` `{count}` `{leftAt}` …',
        ephemeral: true,
      });
    }
  },
};
