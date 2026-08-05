'use strict';

const { Events } = require('discord.js');
const {
  loadWelcomeConfig,
  saveWelcomeConfig,
  upsertButton,
  colorToHex,
  parseColor,
} = require('./configLoader');
const { buildPresenceEmbed } = require('./embeds');

/**
 * Automated Welcome & Goodbye announcer + mutable config store.
 *
 * Large-guild notes:
 * - GuildMembers intent + cached guild.memberCount (no member listing).
 * - Channel resolved from cache first, fetch only on miss.
 * - Leave events may be partial; User is always available.
 */
class WelcomeService {
  constructor(client, welcomeConfig = null) {
    this.client = client;
    this.config = welcomeConfig || loadWelcomeConfig();
  }

  /** Active = toggled on AND channel configured. */
  get enabled() {
    return Boolean(this.config?.enabled && this.config.channelId);
  }

  reload() {
    this.config = loadWelcomeConfig();
    return this.config;
  }

  /**
   * Apply a patch, persist to data/welcome.json, refresh in-memory config.
   * @param {object} patch
   */
  update(patch) {
    const merged = {
      enabled: typeof patch.enabled === 'boolean' ? patch.enabled : this.config.enabled,
      channelId:
        patch.channelId !== undefined ? patch.channelId : this.config.channelId,
      newAccountDays:
        patch.newAccountDays !== undefined
          ? patch.newAccountDays
          : this.config.newAccountDays,
      colors: { ...this.config.colors, ...(patch.colors || {}) },
      images: { ...this.config.images, ...(patch.images || {}) },
      welcome: { ...this.config.welcome, ...(patch.welcome || {}) },
      goodbye: { ...this.config.goodbye, ...(patch.goodbye || {}) },
      _rawButtons: patch._rawButtons || this.config._rawButtons,
      buttons: patch._rawButtons || this.config._rawButtons,
    };

    saveWelcomeConfig(merged);
    this.config = loadWelcomeConfig();
    return this.config;
  }

  setEnabled(value) {
    return this.update({ enabled: Boolean(value) });
  }

  setChannel(channelId) {
    return this.update({ channelId: String(channelId || '') });
  }

  setBanner(url, { both = true } = {}) {
    const images = { ...this.config.images };
    images.welcomeBanner = url || '';
    if (both) images.goodbyeBanner = url || '';
    return this.update({ images });
  }

  setColor(hex, { target = 'both' } = {}) {
    const n = parseColor(hex, null);
    if (n == null && typeof hex === 'string') {
      throw new Error('Invalid color. Use hex like #FF4655.');
    }
    const colors = { ...this.config.colors };
    if (target === 'welcome' || target === 'both') colors.welcome = n;
    if (target === 'goodbye' || target === 'both') colors.goodbye = n;
    return this.update({ colors });
  }

  setWelcomeMessage(description) {
    return this.update({ welcome: { description } });
  }

  setGoodbyeMessage(description) {
    return this.update({ goodbye: { description } });
  }

  setFooter(text, { target = 'both' } = {}) {
    const patch = {};
    if (target === 'welcome' || target === 'both') {
      patch.welcome = { footerText: text };
    }
    if (target === 'goodbye' || target === 'both') {
      patch.goodbye = { ...(patch.goodbye || {}), footerText: text };
    }
    return this.update(patch);
  }

  setLogo(url) {
    return this.update({ images: { serverLogo: url || '' } });
  }

  setButtonChannel(buttonId, channelId) {
    const raw = upsertButton(this.config._rawButtons || [], buttonId, {
      channelId: channelId || '',
      enabled: true,
    });
    return this.update({ _rawButtons: raw });
  }

  setButtonUrl(buttonId, url, { enabled = true, label = null, emoji = null } = {}) {
    const patch = { url: url || '', enabled };
    if (label) patch.label = label;
    if (emoji) patch.emoji = emoji;
    const raw = upsertButton(this.config._rawButtons || [], buttonId, patch);
    return this.update({ _rawButtons: raw });
  }

  /**
   * One-shot setup from slash options.
   */
  setup(options) {
    const patch = {
      enabled: true,
      _rawButtons: [...(this.config._rawButtons || [])],
    };

    if (options.channelId) patch.channelId = options.channelId;

    if (options.banner) {
      patch.images = {
        ...(this.config.images || {}),
        welcomeBanner: options.banner,
        goodbyeBanner: options.banner,
      };
    }
    if (options.logo) {
      patch.images = { ...(patch.images || this.config.images || {}), serverLogo: options.logo };
    }
    if (options.color) {
      const n = parseColor(options.color, null);
      if (n == null) throw new Error('Invalid color. Use hex like #FF4655.');
      patch.colors = { welcome: n, goodbye: this.config.colors.goodbye };
    }
    if (options.footer) {
      patch.welcome = { ...(this.config.welcome || {}), footerText: options.footer };
      patch.goodbye = { ...(this.config.goodbye || {}), footerText: options.footer };
    }

    let raw = patch._rawButtons;
    if (options.rulesChannelId) {
      raw = upsertButton(raw, 'rules', {
        label: 'Rules',
        emoji: '📜',
        channelId: options.rulesChannelId,
        enabled: true,
      });
    }
    if (options.rolesChannelId) {
      raw = upsertButton(raw, 'roles', {
        label: 'Roles',
        emoji: '🎭',
        channelId: options.rolesChannelId,
        enabled: true,
      });
    }
    if (options.generalChannelId) {
      raw = upsertButton(raw, 'general', {
        label: 'General',
        emoji: '💬',
        channelId: options.generalChannelId,
        enabled: true,
      });
    }
    if (options.websiteUrl) {
      raw = upsertButton(raw, 'website', {
        label: 'Website',
        emoji: '🌐',
        url: options.websiteUrl,
        enabled: true,
      });
    }
    if (options.trackerUrl) {
      raw = upsertButton(raw, 'tracker', {
        label: 'Valorant Tracker',
        emoji: '🎮',
        url: options.trackerUrl,
        enabled: true,
      });
    }
    patch._rawButtons = raw;

    return this.update(patch);
  }

  register() {
    if (this.config?._error) {
      console.warn(`[Welcome] Config warning: ${this.config._error}`);
    }

    if (this.enabled) {
      console.log(
        `[Welcome] Enabled · channel=${this.config.channelId} · db=${this.config._source}`,
      );
    } else {
      console.log(
        `[Welcome] Inactive (enabled=${this.config.enabled}, channel=${this.config.channelId || 'none'}). Use /welcome setup`,
      );
    }

    this.client.on(Events.GuildMemberAdd, (member) => {
      this.handleJoin(member).catch((err) => console.error('[Welcome] join failed:', err));
    });

    this.client.on(Events.GuildMemberRemove, (member) => {
      this.handleLeave(member).catch((err) => console.error('[Welcome] leave failed:', err));
    });
  }

  async _resolveChannel(guild) {
    const channelId = this.config.channelId;
    if (!channelId) return null;
    const cached = guild.channels.cache.get(channelId);
    if (cached?.isTextBased()) return cached;
    const fetched = await guild.channels.fetch(channelId).catch(() => null);
    return fetched?.isTextBased() ? fetched : null;
  }

  async handleJoin(member) {
    if (!this.enabled) return;
    if (member.user?.bot) return;

    const channel = await this._resolveChannel(member.guild);
    if (!channel) {
      console.warn(`[Welcome] Channel ${this.config.channelId} missing.`);
      return;
    }

    await channel.send(buildPresenceEmbed('welcome', member, member.guild, this.config));
  }

  async handleLeave(member) {
    if (!this.enabled) return;
    const user = member.user || member;
    if (user?.bot) return;
    const guild = member.guild;
    if (!guild) return;

    const channel = await this._resolveChannel(guild);
    if (!channel) {
      console.warn(`[Welcome] Channel ${this.config.channelId} missing.`);
      return;
    }

    await channel.send(
      buildPresenceEmbed('goodbye', member, guild, this.config, { leftAt: Date.now() }),
    );
  }

  async sendTest(guild, member, kind = 'welcome') {
    const channel = await this._resolveChannel(guild);
    if (!channel) {
      throw new Error('Welcome channel is not set or inaccessible. Use /welcome channel first.');
    }
    const payload = buildPresenceEmbed(kind, member, guild, this.config, {
      leftAt: kind === 'goodbye' ? Date.now() : undefined,
    });
    await channel.send(payload);
    return channel;
  }

  statusLines() {
    const cfg = this.config;
    return [
      `• Active: **${this.enabled}** (toggle=${cfg.enabled})`,
      `• Channel: ${cfg.channelId ? `<#${cfg.channelId}>` : '_not set_'}`,
      `• DB: \`${cfg._source || '—'}\``,
      `• Welcome color: \`${colorToHex(cfg.colors.welcome)}\``,
      `• Goodbye color: \`${colorToHex(cfg.colors.goodbye)}\``,
      `• Banner: ${cfg.images.welcomeBanner ? 'yes' : 'none'}`,
      `• Buttons: **${cfg.buttons.length}** active / **${cfg._rawButtons?.length ?? 0}** total`,
      `• New-account threshold: **${cfg.newAccountDays}d**`,
    ];
  }
}

module.exports = { WelcomeService };
