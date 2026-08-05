'use strict';

const fs = require('node:fs');
const path = require('node:path');

const DATA_PATH = path.join(__dirname, '..', '..', 'data', 'welcome.json');
const EXAMPLE_PATH = path.join(__dirname, '..', '..', 'config', 'welcome.example.json');
const LEGACY_PATH = path.join(__dirname, '..', '..', 'config', 'welcome.json');

const DEFAULT_WELCOME_DESCRIPTION =
  '🎉 Welcome, **{member}**!\n\nWe\'re excited to have you join our community.\n\nRead the rules, customize your roles and enjoy your stay.\n\nGood luck and have fun!';

const DEFAULT_GOODBYE_DESCRIPTION =
  '👋 **{member}** has left the server.\n\nThank you for being part of our community.\n\nWe hope to see you again someday.';

function ensureDataDir() {
  const dir = path.dirname(DATA_PATH);
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
}

function readJson(filePath) {
  if (!fs.existsSync(filePath)) return null;
  return JSON.parse(fs.readFileSync(filePath, 'utf8'));
}

function defaultRaw() {
  if (fs.existsSync(EXAMPLE_PATH)) {
    return readJson(EXAMPLE_PATH);
  }
  return {
    enabled: false,
    channelId: '',
    newAccountDays: 7,
    colors: { welcome: '#5865F2', goodbye: '#ED4245' },
    images: { welcomeBanner: '', goodbyeBanner: '', serverLogo: '' },
    welcome: {
      title: '🎉 Welcome!',
      description: DEFAULT_WELCOME_DESCRIPTION,
      footerText: '{server} • You are member #{count}',
    },
    goodbye: {
      title: '👋 Goodbye',
      description: DEFAULT_GOODBYE_DESCRIPTION,
      footerText: '{server} • {count} members remaining',
    },
    buttons: [],
  };
}

/**
 * Persistent welcome/goodbye "database" (JSON file).
 * Prefer data/welcome.json; migrate legacy config/welcome.json on first run.
 */
function loadWelcomeConfig() {
  ensureDataDir();

  let raw = null;
  let source = DATA_PATH;

  try {
    if (fs.existsSync(DATA_PATH)) {
      raw = readJson(DATA_PATH);
    } else if (fs.existsSync(LEGACY_PATH)) {
      raw = readJson(LEGACY_PATH);
      source = LEGACY_PATH;
      // Migrate to data store
      saveWelcomeConfig(raw);
      source = DATA_PATH;
    } else {
      raw = defaultRaw();
      saveWelcomeConfig(raw);
      source = DATA_PATH;
    }
  } catch (err) {
    return {
      enabled: false,
      channelId: '',
      buttons: [],
      colors: { welcome: 0x5865f2, goodbye: 0xed4245 },
      images: { welcomeBanner: '', goodbyeBanner: '', serverLogo: '' },
      welcome: { title: '🎉 Welcome!', description: DEFAULT_WELCOME_DESCRIPTION },
      goodbye: { title: '👋 Goodbye', description: DEFAULT_GOODBYE_DESCRIPTION },
      _source: null,
      _error: `Failed to load welcome DB: ${err.message}`,
      _raw: null,
    };
  }

  return normalizeWelcomeConfig(raw, source);
}

/**
 * Persist config to data/welcome.json (slash-command edits land here).
 * @param {object} config normalized or raw-ish config
 */
function saveWelcomeConfig(config) {
  ensureDataDir();
  const payload = toPersistedShape(config);
  fs.writeFileSync(DATA_PATH, JSON.stringify(payload, null, 2), 'utf8');
  return payload;
}

function toPersistedShape(config) {
  // Prefer original raw buttons if present (includes disabled)
  const buttons = Array.isArray(config._rawButtons)
    ? config._rawButtons
    : Array.isArray(config.buttons)
      ? config.buttons.map((b) => ({
          id: b.id,
          label: b.label,
          emoji: b.emoji || '',
          enabled: b.enabled !== false,
          channelId: b.channelId || '',
          url: b.url || '',
        }))
      : [];

  const colorStr = (n, fallback) => {
    if (typeof n === 'string') return n.startsWith('#') ? n : `#${n}`;
    if (typeof n === 'number') return `#${(n >>> 0).toString(16).padStart(6, '0')}`;
    return fallback;
  };

  return {
    enabled: config.enabled === true,
    channelId: String(config.channelId || ''),
    newAccountDays:
      Number.isFinite(config.newAccountDays) && config.newAccountDays > 0
        ? config.newAccountDays
        : 7,
    colors: {
      welcome: colorStr(config.colors?.welcome, '#5865F2'),
      goodbye: colorStr(config.colors?.goodbye, '#ED4245'),
    },
    images: {
      welcomeBanner: config.images?.welcomeBanner || '',
      goodbyeBanner: config.images?.goodbyeBanner || '',
      serverLogo: config.images?.serverLogo || '',
    },
    welcome: {
      title: config.welcome?.title || '🎉 Welcome!',
      description: config.welcome?.description || DEFAULT_WELCOME_DESCRIPTION,
      showAvatar: config.welcome?.showAvatar !== false,
      showBanner: config.welcome?.showBanner !== false,
      showMemberCount: config.welcome?.showMemberCount !== false,
      showJoinedAt: config.welcome?.showJoinedAt !== false,
      showAccountAge: config.welcome?.showAccountAge !== false,
      footerText: config.welcome?.footerText || '{server} • You are member #{count}',
    },
    goodbye: {
      title: config.goodbye?.title || '👋 Goodbye',
      description: config.goodbye?.description || DEFAULT_GOODBYE_DESCRIPTION,
      showAvatar: config.goodbye?.showAvatar !== false,
      showBanner: config.goodbye?.showBanner !== false,
      showMemberCount: config.goodbye?.showMemberCount !== false,
      showLeftAt: config.goodbye?.showLeftAt !== false,
      footerText: config.goodbye?.footerText || '{server} • {count} members remaining',
    },
    buttons,
  };
}

function normalizeWelcomeConfig(parsed, source) {
  const colors = parsed.colors || {};
  const images = parsed.images || {};
  const welcome = parsed.welcome || {};
  const goodbye = parsed.goodbye || {};
  const rawButtons = Array.isArray(parsed.buttons) ? parsed.buttons : [];

  const channelId = String(parsed.channelId || '').trim();
  const cleanChannel =
    !channelId || channelId.startsWith('REPLACE_') ? '' : channelId;

  const newAccountDays = Number.parseInt(parsed.newAccountDays ?? 7, 10);

  return {
    enabled: parsed.enabled === true,
    channelId: cleanChannel,
    newAccountDays: Number.isFinite(newAccountDays) && newAccountDays > 0 ? newAccountDays : 7,
    colors: {
      welcome: parseColor(colors.welcome, 0x5865f2),
      goodbye: parseColor(colors.goodbye, 0xed4245),
    },
    images: {
      welcomeBanner: String(images.welcomeBanner || '').trim(),
      goodbyeBanner: String(images.goodbyeBanner || '').trim(),
      serverLogo: String(images.serverLogo || '').trim(),
    },
    welcome: {
      title: welcome.title || '🎉 Welcome!',
      description: welcome.description || DEFAULT_WELCOME_DESCRIPTION,
      showAvatar: welcome.showAvatar !== false,
      showBanner: welcome.showBanner !== false,
      showMemberCount: welcome.showMemberCount !== false,
      showJoinedAt: welcome.showJoinedAt !== false,
      showAccountAge: welcome.showAccountAge !== false,
      footerText: welcome.footerText || '{server} • You are member #{count}',
    },
    goodbye: {
      title: goodbye.title || '👋 Goodbye',
      description: goodbye.description || DEFAULT_GOODBYE_DESCRIPTION,
      showAvatar: goodbye.showAvatar !== false,
      showBanner: goodbye.showBanner !== false,
      showMemberCount: goodbye.showMemberCount !== false,
      showLeftAt: goodbye.showLeftAt !== false,
      footerText: goodbye.footerText || '{server} • {count} members remaining',
    },
    // Active buttons only (for embeds)
    buttons: rawButtons.map(normalizeButton).filter(Boolean),
    // Full list for persistence / setup edits
    _rawButtons: rawButtons.map((b) => ({
      id: String(b.id || b.label || 'btn').toLowerCase().replace(/\s+/g, '-'),
      label: String(b.label || ''),
      emoji: b.emoji ? String(b.emoji) : '',
      enabled: b.enabled !== false,
      channelId: String(b.channelId || '').startsWith('REPLACE_')
        ? ''
        : String(b.channelId || ''),
      url: String(b.url || ''),
    })),
    _source: source,
    _error: null,
  };
}

function normalizeButton(btn) {
  if (!btn || btn.enabled === false) return null;
  const label = String(btn.label || '').trim();
  if (!label) return null;

  const url = String(btn.url || '').trim();
  let channelId = String(btn.channelId || '').trim();
  if (channelId.startsWith('REPLACE_')) channelId = '';
  if (!url && !channelId) return null;
  if (url && !/^https?:\/\//i.test(url)) return null;

  return {
    id: String(btn.id || label).toLowerCase().replace(/\s+/g, '-'),
    label,
    emoji: btn.emoji ? String(btn.emoji) : null,
    url: url || null,
    channelId: channelId || null,
    enabled: true,
  };
}

function parseColor(value, fallback) {
  if (typeof value === 'number' && Number.isFinite(value)) return value >>> 0;
  if (typeof value === 'string') {
    const hex = value.trim().replace(/^#/, '');
    if (/^[0-9a-fA-F]{6}$/.test(hex)) return Number.parseInt(hex, 16);
  }
  return fallback;
}

function colorToHex(n) {
  return `#${(n >>> 0).toString(16).padStart(6, '0')}`;
}

function applyTemplate(template, vars) {
  return String(template).replace(/\{(\w+)\}/g, (match, key) => {
    if (Object.prototype.hasOwnProperty.call(vars, key) && vars[key] != null) {
      return String(vars[key]);
    }
    return match;
  });
}

function upsertButton(rawButtons, id, patch) {
  const list = [...rawButtons];
  const idx = list.findIndex((b) => b.id === id);
  if (idx >= 0) {
    list[idx] = { ...list[idx], ...patch, id };
  } else {
    list.push({
      id,
      label: patch.label || id,
      emoji: patch.emoji || '',
      enabled: patch.enabled !== false,
      channelId: patch.channelId || '',
      url: patch.url || '',
    });
  }
  return list;
}

module.exports = {
  loadWelcomeConfig,
  saveWelcomeConfig,
  normalizeWelcomeConfig,
  toPersistedShape,
  applyTemplate,
  parseColor,
  colorToHex,
  upsertButton,
  DATA_PATH,
  EXAMPLE_PATH,
  DEFAULT_WELCOME_DESCRIPTION,
  DEFAULT_GOODBYE_DESCRIPTION,
};
