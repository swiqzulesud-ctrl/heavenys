'use strict';

require('dotenv').config();

function required(name) {
  const value = process.env[name];
  if (!value || !String(value).trim()) {
    throw new Error(`Variable d'environnement manquante: ${name}`);
  }
  return String(value).trim();
}

function optional(name, fallback = '') {
  const value = process.env[name];
  return value && String(value).trim() ? String(value).trim() : fallback;
}

const cleanupMode = optional('VOICE_CLEANUP_MODE', 'delete').toLowerCase();
if (!['delete', 'archive'].includes(cleanupMode)) {
  throw new Error('VOICE_CLEANUP_MODE doit être "delete" ou "archive"');
}

const teamSize = Number.parseInt(optional('TEAM_SIZE', '5'), 10);
if (!Number.isFinite(teamSize) || teamSize < 1 || teamSize > 10) {
  throw new Error('TEAM_SIZE doit être un entier entre 1 et 10');
}

module.exports = {
  token: required('DISCORD_TOKEN'),
  clientId: required('CLIENT_ID'),
  guildId: required('GUILD_ID'),
  gamesChannelId: required('GAMES_CHANNEL_ID'),
  voiceCategoryId: required('VOICE_CATEGORY_ID'),
  archiveCategoryId: optional('ARCHIVE_CATEGORY_ID'),
  voiceCleanupMode: cleanupMode,
  organizerRoleName: optional('ORGANIZER_ROLE_NAME', 'Organisateur de Parties'),
  teamSize,
};
