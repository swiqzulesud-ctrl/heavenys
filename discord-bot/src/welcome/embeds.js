'use strict';

const {
  EmbedBuilder,
  ActionRowBuilder,
  ButtonBuilder,
  ButtonStyle,
} = require('discord.js');
const { applyTemplate } = require('./configLoader');

function channelUrl(guildId, channelId) {
  return `https://discord.com/channels/${guildId}/${channelId}`;
}

function formatJoinedAt(date) {
  if (!date) return 'Unknown';
  const ms = date instanceof Date ? date.getTime() : Number(date);
  if (!Number.isFinite(ms)) return 'Unknown';
  const unix = Math.floor(ms / 1000);
  return `<t:${unix}:F> (<t:${unix}:R>)`;
}

function resolveLogo(guild, images) {
  if (images.serverLogo) return images.serverLogo;
  return guild.iconURL({ size: 128, extension: 'png' }) || null;
}

function buildVars(memberOrUser, guild, memberCount) {
  const user = memberOrUser.user || memberOrUser;
  const display = memberOrUser.displayName || user.globalName || user.username;
  return {
    member: user.toString(),
    username: user.username,
    display,
    tag: user.tag || `${user.username}`,
    id: user.id,
    server: guild.name,
    count: String(memberCount),
    joinedAt: formatJoinedAt(memberOrUser.joinedAt || memberOrUser.joinedTimestamp),
    createdAt: formatJoinedAt(user.createdAt || user.createdTimestamp),
  };
}

function buildButtons(guildId, buttons) {
  if (!buttons.length) return [];

  // Discord allows max 5 buttons per row; we keep a single row of up to 5.
  const row = new ActionRowBuilder();
  for (const btn of buttons.slice(0, 5)) {
    const builder = new ButtonBuilder()
      .setStyle(ButtonStyle.Link)
      .setLabel(btn.label);

    if (btn.emoji) {
      try {
        builder.setEmoji(btn.emoji);
      } catch {
        // Invalid emoji in config — skip emoji, keep the button.
      }
    }

    const url = btn.url || (btn.channelId ? channelUrl(guildId, btn.channelId) : null);
    if (!url) continue;
    builder.setURL(url);
    row.addComponents(builder);
  }

  return row.components.length ? [row] : [];
}

/**
 * @param {'welcome'|'goodbye'} kind
 * @param {import('discord.js').GuildMember|import('discord.js').User|object} memberOrUser
 * @param {import('discord.js').Guild} guild
 * @param {object} config normalized welcome config
 */
function buildPresenceEmbed(kind, memberOrUser, guild, config) {
  const user = memberOrUser.user || memberOrUser;
  const section = kind === 'welcome' ? config.welcome : config.goodbye;
  const color = kind === 'welcome' ? config.colors.welcome : config.colors.goodbye;
  const banner =
    kind === 'welcome' ? config.images.welcomeBanner : config.images.goodbyeBanner;

  // Prefer live cache count (cheap); works well on large guilds.
  const memberCount = guild.memberCount ?? 0;
  const vars = buildVars(memberOrUser, guild, memberCount);

  const embed = new EmbedBuilder()
    .setColor(color)
    .setTitle(applyTemplate(section.title, vars))
    .setDescription(applyTemplate(section.description, vars))
    .setTimestamp(new Date());

  if (section.showAvatar) {
    const avatar = user.displayAvatarURL({ size: 256, extension: 'png' });
    embed.setThumbnail(avatar);
  }

  if (section.showBanner && banner) {
    embed.setImage(banner);
  }

  const logo = resolveLogo(guild, config.images);
  const footerText = applyTemplate(section.footerText, vars);
  if (logo) {
    embed.setFooter({ text: footerText, iconURL: logo });
  } else {
    embed.setFooter({ text: footerText });
  }

  // Optional author line with server branding
  if (logo) {
    embed.setAuthor({ name: guild.name, iconURL: logo });
  } else {
    embed.setAuthor({ name: guild.name });
  }

  if (section.showMemberCount) {
    embed.addFields({
      name: 'Members',
      value: `**${memberCount}**`,
      inline: true,
    });
  }

  if (kind === 'welcome' && section.showJoinedAt) {
    embed.addFields({
      name: 'Joined',
      value: vars.joinedAt,
      inline: true,
    });
  }

  if (kind === 'goodbye') {
    embed.addFields({
      name: 'User',
      value: `\`${vars.username}\` · \`${vars.id}\``,
      inline: true,
    });
  }

  return {
    embeds: [embed],
    components: buildButtons(guild.id, config.buttons),
  };
}

module.exports = {
  buildPresenceEmbed,
  buildButtons,
  buildVars,
  applyTemplate,
  channelUrl,
  formatJoinedAt,
};
