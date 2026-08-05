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

function formatTimestamp(date) {
  if (!date) return 'Unknown';
  const ms = date instanceof Date ? date.getTime() : Number(date);
  if (!Number.isFinite(ms)) return 'Unknown';
  const unix = Math.floor(ms / 1000);
  return `<t:${unix}:F> (<t:${unix}:R>)`;
}

/** Prefer animated GIF avatars when the user has one. */
function avatarUrl(user, size = 256) {
  if (!user?.displayAvatarURL) return null;
  return user.displayAvatarURL({ size, forceStatic: false });
}

function resolveLogo(guild, images) {
  if (images.serverLogo) return images.serverLogo;
  return guild.iconURL({ size: 128, forceStatic: false }) || null;
}

function accountAgeDays(user) {
  const created = user.createdAt || user.createdTimestamp;
  if (!created) return null;
  const ms = created instanceof Date ? created.getTime() : Number(created);
  if (!Number.isFinite(ms)) return null;
  return Math.floor((Date.now() - ms) / (24 * 60 * 60 * 1000));
}

function isNewAccount(user, thresholdDays) {
  const age = accountAgeDays(user);
  if (age == null) return false;
  return age < thresholdDays;
}

function buildVars(memberOrUser, guild, memberCount, leftAt = null) {
  const user = memberOrUser.user || memberOrUser;
  const display = memberOrUser.displayName || user.globalName || user.username;
  const age = accountAgeDays(user);
  return {
    member: typeof user.toString === 'function' ? user.toString() : `<@${user.id}>`,
    username: user.username,
    display,
    tag: user.tag || `${user.username}`,
    id: user.id,
    server: guild.name,
    count: String(memberCount),
    joinedAt: formatTimestamp(memberOrUser.joinedAt || memberOrUser.joinedTimestamp),
    createdAt: formatTimestamp(user.createdAt || user.createdTimestamp),
    leftAt: formatTimestamp(leftAt || Date.now()),
    accountAge: age == null ? 'Unknown' : `${age} day${age === 1 ? '' : 's'}`,
  };
}

function buildButtons(guildId, buttons) {
  if (!buttons.length) return [];

  const rows = [];
  let row = new ActionRowBuilder();

  for (const btn of buttons) {
    if (row.components.length >= 5) {
      rows.push(row);
      row = new ActionRowBuilder();
    }

    const builder = new ButtonBuilder().setStyle(ButtonStyle.Link).setLabel(btn.label);
    if (btn.emoji) {
      try {
        builder.setEmoji(btn.emoji);
      } catch {
        /* ignore bad emoji */
      }
    }

    const url = btn.url || (btn.channelId ? channelUrl(guildId, btn.channelId) : null);
    if (!url || url.length > 512) continue;
    builder.setURL(url);
    row.addComponents(builder);
  }

  if (row.components.length) rows.push(row);
  return rows;
}

/**
 * @param {'welcome'|'goodbye'} kind
 */
function buildPresenceEmbed(kind, memberOrUser, guild, config, options = {}) {
  const user = memberOrUser.user || memberOrUser;
  const section = kind === 'welcome' ? config.welcome : config.goodbye;
  const color = kind === 'welcome' ? config.colors.welcome : config.colors.goodbye;
  const banner =
    kind === 'welcome' ? config.images.welcomeBanner : config.images.goodbyeBanner;

  const memberCount = guild.memberCount ?? 0;
  const vars = buildVars(memberOrUser, guild, memberCount, options.leftAt);

  const embed = new EmbedBuilder()
    .setColor(color)
    .setTitle(applyTemplate(section.title, vars))
    .setDescription(applyTemplate(section.description, vars))
    .setTimestamp(options.leftAt ? new Date(options.leftAt) : new Date());

  if (section.showAvatar) {
    const avatar = avatarUrl(user, 256);
    if (avatar) embed.setThumbnail(avatar);
  }

  if (section.showBanner && banner) {
    embed.setImage(banner);
  }

  const logo = resolveLogo(guild, config.images);
  const footerText = applyTemplate(section.footerText, vars);
  embed.setFooter(logo ? { text: footerText, iconURL: logo } : { text: footerText });
  embed.setAuthor(logo ? { name: guild.name, iconURL: logo } : { name: guild.name });

  if (section.showMemberCount) {
    embed.addFields({ name: 'Members', value: `**${memberCount}**`, inline: true });
  }

  if (kind === 'welcome' && section.showJoinedAt) {
    embed.addFields({ name: 'Joined', value: vars.joinedAt, inline: true });
  }

  if (kind === 'goodbye' && section.showLeftAt !== false) {
    embed.addFields({ name: 'Left', value: vars.leftAt, inline: true });
  }

  if (kind === 'welcome' && section.showAccountAge !== false) {
    const threshold = config.newAccountDays || 7;
    if (isNewAccount(user, threshold)) {
      embed.addFields({
        name: '⚠️ New account',
        value: `Created ${vars.accountAge} ago (< ${threshold}d)`,
        inline: true,
      });
    } else {
      embed.addFields({
        name: 'Account age',
        value: vars.accountAge,
        inline: true,
      });
    }
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
  formatTimestamp,
  avatarUrl,
  isNewAccount,
  accountAgeDays,
};
