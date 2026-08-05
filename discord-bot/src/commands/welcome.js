'use strict';

const { SlashCommandBuilder, PermissionFlagsBits } = require('discord.js');
const { buildPresenceEmbed } = require('../welcome/embeds');

module.exports = {
  data: new SlashCommandBuilder()
    .setName('welcome')
    .setDescription('Welcome & Goodbye system controls')
    .setDefaultMemberPermissions(PermissionFlagsBits.ManageGuild)
    .addSubcommand((sub) =>
      sub
        .setName('reload')
        .setDescription('Reload config/welcome.json without restarting the bot'),
    )
    .addSubcommand((sub) =>
      sub
        .setName('preview')
        .setDescription('Preview the welcome or goodbye embed in this channel')
        .addStringOption((opt) =>
          opt
            .setName('type')
            .setDescription('Which embed to preview')
            .setRequired(true)
            .addChoices(
              { name: 'Welcome', value: 'welcome' },
              { name: 'Goodbye', value: 'goodbye' },
            ),
        ),
    )
    .addSubcommand((sub) =>
      sub.setName('status').setDescription('Show welcome system status'),
    ),

  /**
   * @param {import('discord.js').ChatInputCommandInteraction} interaction
   * @param {{ welcomeService: import('../welcome/WelcomeService').WelcomeService }} ctx
   */
  async execute(interaction, ctx) {
    const { welcomeService } = ctx;
    if (!welcomeService) {
      return interaction.reply({
        content: 'Welcome service is not initialized.',
        ephemeral: true,
      });
    }

    const sub = interaction.options.getSubcommand();

    if (sub === 'reload') {
      const cfg = welcomeService.reload();
      if (cfg._error) {
        return interaction.reply({
          content: `Reload failed: ${cfg._error}`,
          ephemeral: true,
        });
      }
      return interaction.reply({
        content:
          `Config reloaded from \`${cfg._source || 'unknown'}\`.\n` +
          `Enabled: **${welcomeService.enabled}** · Channel: \`${cfg.channelId || '—'}\` · Buttons: **${cfg.buttons.length}**`,
        ephemeral: true,
      });
    }

    if (sub === 'status') {
      const cfg = welcomeService.config;
      return interaction.reply({
        content:
          `**Welcome & Goodbye**\n` +
          `• Enabled: **${welcomeService.enabled}**\n` +
          `• Channel: \`${cfg.channelId || 'not set'}\`\n` +
          `• Source: \`${cfg._source || '—'}\`\n` +
          `• Buttons: **${cfg.buttons?.length ?? 0}**\n` +
          `• Welcome color: \`#${cfg.colors.welcome.toString(16).padStart(6, '0')}\`\n` +
          `• Goodbye color: \`#${cfg.colors.goodbye.toString(16).padStart(6, '0')}\``,
        ephemeral: true,
      });
    }

    if (sub === 'preview') {
      if (!welcomeService.enabled && !welcomeService.config.channelId) {
        // Allow preview even if placeholder channel — use current guild branding.
      }
      const kind = interaction.options.getString('type');
      const payload = buildPresenceEmbed(
        kind,
        interaction.member,
        interaction.guild,
        {
          ...welcomeService.config,
          // Ensure preview works even before channelId is configured
          enabled: true,
        },
      );
      return interaction.reply({ ...payload, ephemeral: true });
    }
  },
};
