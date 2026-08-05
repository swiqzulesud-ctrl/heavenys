'use strict';

const fs = require('node:fs');
const path = require('node:path');

const DEFAULT_PATH = path.join(__dirname, '..', '..', 'config', 'welcome.json');
const EXAMPLE_PATH = path.join(__dirname, '..', '..', 'config', 'welcome.example.json');

/**
 * Load welcome/goodbye settings from JSON (no code changes needed to customize).
 * Falls back to the example file when welcome.json is missing.
 */
function loadWelcomeConfig(filePath = DEFAULT_PATH) {
  const candidates = [filePath, EXAMPLE_PATH];
  let raw = null;
  let used = null;

  for (const candidate of candidates) {
    if (fs.existsSync(candidate)) {
      raw = fs.readFileSync(candidate, 'utf8');
      used = candidate;
      break;
    }
  }

  if (!raw) {
    return {
      enabled: false,
      _source: null,
      _error: 'No welcome config found (config/welcome.json)',
    };
  }

  let parsed;
  try {
    parsed = JSON.parse(raw);
  } catch (err) {
    return {
      enabled: false,
      _source: used,
      _error: `Invalid JSON in ${used}: ${err.message}`,
    };
  }

  return normalizeWelcomeConfig(parsed, used);
}

function normalizeWelcomeConfig(parsed, source) {
  const colors = parsed.colors || {};
  const images = parsed.images || {};
  const welcome = parsed.welcome || {};
  const goodbye = parsed.goodbye || {};
  const buttons = Array.isArray(parsed.buttons) ? parsed.buttons : [];

  const channelId = String(parsed.channelId || '').trim();
  const looksLikePlaceholder = channelId.startsWith('REPLACE_') || !channelId;

  return {
    enabled: parsed.enabled !== false && !looksLikePlaceholder,
    channelId: looksLikePlaceholder ? '' : channelId,
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
      description:
        welcome.description ||
        '🎉 Welcome, **{member}**!\nWe\'re excited to have you join our community.',
      showAvatar: welcome.showAvatar !== false,
      showBanner: welcome.showBanner !== false,
      showMemberCount: welcome.showMemberCount !== false,
      showJoinedAt: welcome.showJoinedAt !== false,
      footerText: welcome.footerText || '{server} • Member #{count}',
    },
    goodbye: {
      title: goodbye.title || '👋 Goodbye',
      description:
        goodbye.description ||
        '👋 **{member}** has left the server.\nThank you for being part of our community.',
      showAvatar: goodbye.showAvatar !== false,
      showBanner: goodbye.showBanner !== false,
      showMemberCount: goodbye.showMemberCount !== false,
      footerText: goodbye.footerText || '{server} • {count} members remaining',
    },
    buttons: buttons.map(normalizeButton).filter(Boolean),
    _source: source,
    _error: null,
  };
}

function normalizeButton(btn) {
  if (!btn || btn.enabled === false) return null;
  const label = String(btn.label || '').trim();
  if (!label) return null;

  const url = String(btn.url || '').trim();
  const channelId = String(btn.channelId || '').trim();
  if (channelId.startsWith('REPLACE_')) return null;
  if (!url && !channelId) return null;
  if (url && !/^https?:\/\//i.test(url)) return null;

  return {
    id: String(btn.id || label).toLowerCase().replace(/\s+/g, '-'),
    label,
    emoji: btn.emoji ? String(btn.emoji) : null,
    url: url || null,
    channelId: channelId || null,
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

/**
 * Replace {placeholders} in a template string.
 * Unknown keys are left as-is.
 */
function applyTemplate(template, vars) {
  return String(template).replace(/\{(\w+)\}/g, (match, key) => {
    if (Object.prototype.hasOwnProperty.call(vars, key) && vars[key] != null) {
      return String(vars[key]);
    }
    return match;
  });
}

module.exports = {
  loadWelcomeConfig,
  normalizeWelcomeConfig,
  applyTemplate,
  parseColor,
  DEFAULT_PATH,
  EXAMPLE_PATH,
};
