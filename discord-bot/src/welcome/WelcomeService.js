'use strict';

const { Events } = require('discord.js');
const { loadWelcomeConfig } = require('./configLoader');
const { buildPresenceEmbed } = require('./embeds');

/**
 * Automated Welcome & Goodbye announcer.
 * Uses a single dedicated channel; fully driven by config/welcome.json.
 *
 * Large-guild notes:
 * - Relies on GuildMembers intent + cached guild.memberCount (no member listing).
 * - Channel is resolved once and refreshed lazily if missing from cache.
 * - Leave events may provide a partial User when the member was not cached.
 */
class WelcomeService {
  /**
   * @param {import('discord.js').Client} client
   * @param {object} [welcomeConfig] preloaded config (optional)
   */
  constructor(client, welcomeConfig = null) {
    this.client = client;
    this.config = welcomeConfig || loadWelcomeConfig();
    /** @type {Map<string, string>} guildId → last known channel id (for multi-guild later) */
    this._channelCache = new Map();
  }

  get enabled() {
    return Boolean(this.config?.enabled && this.config.channelId);
  }

  /** Hot-reload config/welcome.json without restarting the process. */
  reload() {
    this.config = loadWelcomeConfig();
    this._channelCache.clear();
    return this.config;
  }

  register() {
    if (this.config?._error) {
      console.warn(`[Welcome] Config warning: ${this.config._error}`);
    }

    if (this.enabled) {
      console.log(
        `[Welcome] Enabled · channel=${this.config.channelId} · source=${this.config._source}`,
      );
    } else {
      console.log(
        '[Welcome] Disabled — copy config/welcome.example.json → config/welcome.json and set channelId. Use /welcome reload after editing.',
      );
    }

    // Always attach listeners; handlers no-op when disabled so /welcome reload can enable live.
    this.client.on(Events.GuildMemberAdd, (member) => {
      this.handleJoin(member).catch((err) => {
        console.error('[Welcome] join failed:', err);
      });
    });

    this.client.on(Events.GuildMemberRemove, (member) => {
      this.handleLeave(member).catch((err) => {
        console.error('[Welcome] leave failed:', err);
      });
    });
  }

  async _resolveChannel(guild) {
    const channelId = this.config.channelId;
    if (!channelId) return null;

    const cached = guild.channels.cache.get(channelId);
    if (cached?.isTextBased()) return cached;

    const fetched = await guild.channels.fetch(channelId).catch(() => null);
    if (fetched?.isTextBased()) return fetched;
    return null;
  }

  async handleJoin(member) {
    if (!this.enabled) return;
    if (member.user?.bot) return;

    const channel = await this._resolveChannel(member.guild);
    if (!channel) {
      console.warn(`[Welcome] Channel ${this.config.channelId} introuvable.`);
      return;
    }

    const payload = buildPresenceEmbed('welcome', member, member.guild, this.config);
    await channel.send(payload);
  }

  async handleLeave(member) {
    if (!this.enabled) return;
    // member may be a GuildMember or a partial; user is always present on leave.
    const user = member.user || member;
    if (user?.bot) return;

    const guild = member.guild;
    if (!guild) return;

    const channel = await this._resolveChannel(guild);
    if (!channel) {
      console.warn(`[Welcome] Channel ${this.config.channelId} introuvable.`);
      return;
    }

    const payload = buildPresenceEmbed('goodbye', member, guild, this.config);
    await channel.send(payload);
  }
}

module.exports = { WelcomeService };
