'use strict';

const {
  SlashCommandBuilder,
  PermissionFlagsBits,
  ChannelType,
} = require('discord.js');
const { buildPresenceEmbed } = require('../welcome/embeds');
const { parseColor } = require('../welcome/configLoader');

module.exports = {
  data: new SlashCommandBuilder()
    .setName('welcome')
    .setDescription('Welcome & Goodbye system — setup and customization')
    .setDefaultMemberPermissions(PermissionFlagsBits.ManageGuild)
    .addSubcommand((sub) =>
      sub
        .setName('setup')
        .setDescription('Configure channel, banner, color, footer and navigation buttons')
        .addChannelOption((opt) =>
          opt
            .setName('channel')
            .setDescription('Salon Welcome & Goodbye')
            .addChannelTypes(ChannelType.GuildText, ChannelType.GuildAnnouncement)
            .setRequired(true),
        )
        .addStringOption((opt) =>
          opt.setName('banner').setDescription('URL de la bannière (background)').setRequired(false),
        )
        .addStringOption((opt) =>
          opt.setName('color').setDescription('Couleur welcome (hex, ex. #FF4655)').setRequired(false),
        )
        .addStringOption((opt) =>
          opt.setName('footer').setDescription('Texte du footer').setRequired(false),
        )
        .addStringOption((opt) =>
          opt.setName('logo').setDescription('URL du logo serveur').setRequired(false),
        )
        .addChannelOption((opt) =>
          opt
            .setName('rules')
            .setDescription('Salon des règles (bouton 📜)')
            .addChannelTypes(ChannelType.GuildText, ChannelType.GuildAnnouncement)
            .setRequired(false),
        )
        .addChannelOption((opt) =>
          opt
            .setName('roles')
            .setDescription('Salon des rôles (bouton 🎭)')
            .addChannelTypes(ChannelType.GuildText, ChannelType.GuildAnnouncement)
            .setRequired(false),
        )
        .addChannelOption((opt) =>
          opt
            .setName('general')
            .setDescription('Salon général (bouton 💬)')
            .addChannelTypes(ChannelType.GuildText, ChannelType.GuildAnnouncement)
            .setRequired(false),
        )
        .addStringOption((opt) =>
          opt.setName('website').setDescription('URL du site (bouton 🌐)').setRequired(false),
        )
        .addStringOption((opt) =>
          opt
            .setName('tracker')
            .setDescription('URL Valorant Tracker (bouton 🎮)')
            .setRequired(false),
        ),
    )
    .addSubcommand((sub) =>
      sub
        .setName('channel')
        .setDescription('Set the welcome/goodbye announcement channel')
        .addChannelOption((opt) =>
          opt
            .setName('channel')
            .setDescription('Target channel')
            .addChannelTypes(ChannelType.GuildText, ChannelType.GuildAnnouncement)
            .setRequired(true),
        ),
    )
    .addSubcommand((sub) =>
      sub
        .setName('banner')
        .setDescription('Set the banner / background image URL')
        .addStringOption((opt) =>
          opt.setName('url').setDescription('Image URL (https://…)').setRequired(true),
        )
        .addBooleanOption((opt) =>
          opt
            .setName('goodbye_too')
            .setDescription('Also apply to goodbye embeds (default: true)')
            .setRequired(false),
        ),
    )
    .addSubcommand((sub) =>
      sub
        .setName('color')
        .setDescription('Change embed color')
        .addStringOption((opt) =>
          opt.setName('hex').setDescription('Hex color, e.g. #FF4655').setRequired(true),
        )
        .addStringOption((opt) =>
          opt
            .setName('target')
            .setDescription('Which embed color to change')
            .setRequired(false)
            .addChoices(
              { name: 'Both', value: 'both' },
              { name: 'Welcome only', value: 'welcome' },
              { name: 'Goodbye only', value: 'goodbye' },
            ),
        ),
    )
    .addSubcommand((sub) =>
      sub
        .setName('message')
        .setDescription('Edit the welcome message text (supports {member}, {server}, …)')
        .addStringOption((opt) =>
          opt.setName('text').setDescription('New welcome description').setRequired(true),
        ),
    )
    .addSubcommand((sub) =>
      sub
        .setName('footer')
        .setDescription('Edit the embed footer text')
        .addStringOption((opt) =>
          opt.setName('text').setDescription('Footer template').setRequired(true),
        ),
    )
    .addSubcommand((sub) =>
      sub
        .setName('logo')
        .setDescription('Set the server logo URL used in author/footer')
        .addStringOption((opt) =>
          opt.setName('url').setDescription('Image URL (empty to use guild icon)').setRequired(false),
        ),
    )
    .addSubcommand((sub) =>
      sub
        .setName('preview')
        .setDescription('Ephemeral preview of the embed')
        .addStringOption((opt) =>
          opt
            .setName('type')
            .setDescription('Welcome or goodbye')
            .setRequired(false)
            .addChoices(
              { name: 'Welcome', value: 'welcome' },
              { name: 'Goodbye', value: 'goodbye' },
            ),
        ),
    )
    .addSubcommand((sub) =>
      sub
        .setName('test')
        .setDescription('Send a real test message to the welcome channel')
        .addStringOption((opt) =>
          opt
            .setName('type')
            .setDescription('Welcome or goodbye')
            .setRequired(false)
            .addChoices(
              { name: 'Welcome', value: 'welcome' },
              { name: 'Goodbye', value: 'goodbye' },
            ),
        ),
    )
    .addSubcommand((sub) =>
      sub.setName('enable').setDescription('Enable the welcome/goodbye system'),
    )
    .addSubcommand((sub) =>
      sub.setName('disable').setDescription('Disable the welcome/goodbye system'),
    )
    .addSubcommand((sub) =>
      sub.setName('status').setDescription('Show current configuration'),
    )
    .addSubcommand((sub) =>
      sub
        .setName('reload')
        .setDescription('Reload data/welcome.json from disk'),
    ),

  async execute(interaction, ctx) {
    const { welcomeService } = ctx;
    if (!welcomeService) {
      return interaction.reply({ content: 'Welcome service unavailable.', ephemeral: true });
    }

    const sub = interaction.options.getSubcommand();

    try {
      if (sub === 'setup') {
        const channel = interaction.options.getChannel('channel', true);
        const banner = interaction.options.getString('banner');
        const color = interaction.options.getString('color');
        const footer = interaction.options.getString('footer');
        const logo = interaction.options.getString('logo');
        const rules = interaction.options.getChannel('rules');
        const roles = interaction.options.getChannel('roles');
        const general = interaction.options.getChannel('general');
        const website = interaction.options.getString('website');
        const tracker = interaction.options.getString('tracker');

        if (color && parseColor(color, null) == null) {
          return interaction.reply({
            content: 'Invalid color. Use hex like `#FF4655`.',
            ephemeral: true,
          });
        }
        for (const [name, url] of [
          ['banner', banner],
          ['logo', logo],
          ['website', website],
          ['tracker', tracker],
        ]) {
          if (url && !/^https?:\/\//i.test(url)) {
            return interaction.reply({
              content: `\`${name}\` must be an http(s) URL.`,
              ephemeral: true,
            });
          }
        }

        welcomeService.setup({
          channelId: channel.id,
          banner,
          color,
          footer,
          logo,
          rulesChannelId: rules?.id,
          rolesChannelId: roles?.id,
          generalChannelId: general?.id,
          websiteUrl: website,
          trackerUrl: tracker,
        });

        return interaction.reply({
          content:
            `✅ Welcome system configured & **enabled**.\n` +
            `Channel: ${channel}\n` +
            welcomeService.statusLines().join('\n'),
          ephemeral: true,
        });
      }

      if (sub === 'channel') {
        const channel = interaction.options.getChannel('channel', true);
        welcomeService.setChannel(channel.id);
        return interaction.reply({
          content: `Welcome channel set to ${channel}.`,
          ephemeral: true,
        });
      }

      if (sub === 'banner') {
        const url = interaction.options.getString('url', true);
        if (!/^https?:\/\//i.test(url)) {
          return interaction.reply({ content: 'URL must start with http(s)://', ephemeral: true });
        }
        const both = interaction.options.getBoolean('goodbye_too') !== false;
        welcomeService.setBanner(url, { both });
        return interaction.reply({
          content: `Banner updated${both ? ' (welcome + goodbye)' : ' (welcome only)'}.`,
          ephemeral: true,
        });
      }

      if (sub === 'color') {
        const hex = interaction.options.getString('hex', true);
        const target = interaction.options.getString('target') || 'both';
        if (parseColor(hex, null) == null) {
          return interaction.reply({
            content: 'Invalid color. Use hex like `#FF4655`.',
            ephemeral: true,
          });
        }
        welcomeService.setColor(hex, { target });
        return interaction.reply({
          content: `Color set to \`${hex}\` (${target}).`,
          ephemeral: true,
        });
      }

      if (sub === 'message') {
        const text = interaction.options.getString('text', true);
        welcomeService.setWelcomeMessage(text);
        return interaction.reply({
          content: 'Welcome message updated. Placeholders: `{member}` `{server}` `{count}` …',
          ephemeral: true,
        });
      }

      if (sub === 'footer') {
        const text = interaction.options.getString('text', true);
        welcomeService.setFooter(text, { target: 'both' });
        return interaction.reply({ content: 'Footer updated.', ephemeral: true });
      }

      if (sub === 'logo') {
        const url = interaction.options.getString('url') || '';
        if (url && !/^https?:\/\//i.test(url)) {
          return interaction.reply({ content: 'URL must start with http(s)://', ephemeral: true });
        }
        welcomeService.setLogo(url);
        return interaction.reply({
          content: url ? 'Logo updated.' : 'Logo cleared (guild icon will be used).',
          ephemeral: true,
        });
      }

      if (sub === 'preview') {
        const kind = interaction.options.getString('type') || 'welcome';
        const payload = buildPresenceEmbed(kind, interaction.member, interaction.guild, {
          ...welcomeService.config,
          enabled: true,
        }, kind === 'goodbye' ? { leftAt: Date.now() } : {});
        return interaction.reply({ ...payload, ephemeral: true });
      }

      if (sub === 'test') {
        const kind = interaction.options.getString('type') || 'welcome';
        await interaction.deferReply({ ephemeral: true });
        try {
          const channel = await welcomeService.sendTest(
            interaction.guild,
            interaction.member,
            kind,
          );
          return interaction.editReply({
            content: `Test **${kind}** sent to ${channel}.`,
          });
        } catch (err) {
          return interaction.editReply({ content: err.message });
        }
      }

      if (sub === 'enable') {
        if (!welcomeService.config.channelId) {
          return interaction.reply({
            content: 'Set a channel first with `/welcome channel` or `/welcome setup`.',
            ephemeral: true,
          });
        }
        welcomeService.setEnabled(true);
        return interaction.reply({ content: '✅ Welcome & Goodbye **enabled**.', ephemeral: true });
      }

      if (sub === 'disable') {
        welcomeService.setEnabled(false);
        return interaction.reply({
          content: '⏸️ Welcome & Goodbye **disabled**.',
          ephemeral: true,
        });
      }

      if (sub === 'status') {
        return interaction.reply({
          content: `**Welcome & Goodbye**\n${welcomeService.statusLines().join('\n')}`,
          ephemeral: true,
        });
      }

      if (sub === 'reload') {
        const cfg = welcomeService.reload();
        if (cfg._error) {
          return interaction.reply({ content: `Reload failed: ${cfg._error}`, ephemeral: true });
        }
        return interaction.reply({
          content: `Reloaded from \`${cfg._source}\`.\n${welcomeService.statusLines().join('\n')}`,
          ephemeral: true,
        });
      }
    } catch (err) {
      console.error('[welcome cmd]', err);
      const msg = { content: `Error: ${err.message}`, ephemeral: true };
      if (interaction.deferred || interaction.replied) return interaction.followUp(msg);
      return interaction.reply(msg);
    }
  },
};
