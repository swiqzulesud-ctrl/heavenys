'use strict';

/**
 * Lightweight sanity checks (no Discord connection).
 * Run: node src/selfcheck.js
 */

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { parseKda, parseScore, validateMatchScores } = require('./utils/score');
const {
  gameLabel,
  voiceChannelName,
  buildGameEmbed,
  buildGameComponents,
  buildVoiceLinkRow,
  channelUrl,
} = require('./utils/embeds');
const { GameStore } = require('./services/GameStore');

assert.deepEqual(parseKda('24/12/5'), { kills: 24, deaths: 12, assists: 5 });
assert.deepEqual(parseKda('10-3-7'), { kills: 10, deaths: 3, assists: 7 });
assert.equal(parseKda('bad'), null);
assert.equal(parseScore('13'), 13);
assert.equal(parseScore('-1'), null);
assert.ok(validateMatchScores(13, 13));
assert.equal(validateMatchScores(14, 12), null);

assert.equal(gameLabel(0), 'Game A');
assert.equal(gameLabel(1), 'Game B');
assert.equal(voiceChannelName(1, { multi: false }), '🎧 Équipe 1');
assert.equal(voiceChannelName(2, { multi: true, slotIndex: 0 }), '🎧 Équipe 2 - Game A');
assert.equal(channelUrl('111', '222'), 'https://discord.com/channels/111/222');

const sampleGame = {
  id: 'abcdef12-3456-7890-abcd-ef1234567890',
  status: 'open',
  teamSize: 5,
  team1: [{ id: '1' }],
  team2: [],
  organizerTag: 'Org#0001',
  createdAt: Date.now(),
  slotIndex: 0,
  voiceChannel1Id: 'vc1',
  voiceChannel2Id: 'vc2',
  guildId: 'guild1',
  inviteUrl: null,
  lobbyName: 'Heavenys Cust',
  lobbyCode: 'ABCD',
};

assert.ok(buildGameEmbed(sampleGame).data.title.includes('Partie'));
const rows = buildGameComponents(sampleGame);
assert.equal(rows.length, 2);
assert.equal(rows[1].components[0].data.custom_id, `game:voice:${sampleGame.id}`);

const withInvite = { ...sampleGame, inviteUrl: 'https://example.com/join' };
assert.equal(buildGameComponents(withInvite)[1].components[1].data.style, 5);
assert.equal(
  buildVoiceLinkRow('guild1', 'vc1').components[0].data.url,
  'https://discord.com/channels/guild1/vc1',
);

const store = new GameStore();
store.upsert({
  id: 'test-game-selfcheck',
  status: 'open',
  teamSize: 5,
  team1: [],
  team2: [],
  joinOrder: [],
  organizerId: '0',
  organizerTag: 't',
  slotIndex: 0,
  multi: false,
  createdAt: Date.now(),
});
assert.ok(store.get('test-game-selfcheck'));
store.delete('test-game-selfcheck');

const team1 = [];
const team2 = [];
for (let i = 0; i < 10; i++) {
  let team = i % 2 === 0 ? 1 : 2;
  if (team === 1 && team1.length >= 5) team = 2;
  if (team === 2 && team2.length >= 5) team = 1;
  (team === 1 ? team1 : team2).push(i);
}
assert.deepEqual(team1, [0, 2, 4, 6, 8]);
assert.deepEqual(team2, [1, 3, 5, 7, 9]);

// --- Welcome & Goodbye ---
const {
  normalizeWelcomeConfig,
  saveWelcomeConfig,
  applyTemplate,
  parseColor,
  colorToHex,
  upsertButton,
  DATA_PATH,
  EXAMPLE_PATH,
} = require('./welcome/configLoader');
const {
  buildPresenceEmbed,
  isNewAccount,
  avatarUrl,
} = require('./welcome/embeds');
const { WelcomeService } = require('./welcome/WelcomeService');

assert.equal(parseColor('#FF4655', 0), 0xff4655);
assert.equal(colorToHex(0xff4655), '#ff4655');
assert.equal(
  applyTemplate('Hi {member} on {server}', { member: '@u', server: 'Heavenys' }),
  'Hi @u on Heavenys',
);

const welcomeCfg = normalizeWelcomeConfig(
  {
    enabled: true,
    channelId: '999888777',
    newAccountDays: 7,
    colors: { welcome: '#5865F2', goodbye: '#ED4245' },
    images: {
      welcomeBanner: 'https://example.com/welcome.png',
      goodbyeBanner: 'https://example.com/bye.png',
      serverLogo: 'https://example.com/logo.png',
    },
    welcome: {
      title: '🎉 Welcome!',
      description:
        "🎉 Welcome, **{member}**!\n\nWe're excited to have you join our community.\n\nRead the rules, customize your roles and enjoy your stay.\n\nGood luck and have fun!",
      footerText: '{server} • You are member #{count}',
      showAccountAge: true,
    },
    goodbye: {
      title: '👋 Goodbye',
      description:
        '👋 **{member}** has left the server.\n\nThank you for being part of our community.\n\nWe hope to see you again someday.',
      footerText: '{server} • {count} members remaining',
      showLeftAt: true,
    },
    buttons: [
      { id: 'rules', label: 'Rules', emoji: '📜', enabled: true, channelId: '111' },
      { id: 'roles', label: 'Roles', emoji: '🎭', enabled: true, channelId: '222' },
      { id: 'general', label: 'General', emoji: '💬', enabled: true, channelId: '333' },
      { id: 'website', label: 'Website', emoji: '🌐', enabled: true, url: 'https://example.com' },
      {
        id: 'tracker',
        label: 'Valorant Tracker',
        emoji: '🎮',
        enabled: true,
        url: 'https://tracker.gg/valorant',
      },
    ],
  },
  'selfcheck',
);

assert.equal(welcomeCfg.enabled, true);
assert.equal(welcomeCfg.buttons.length, 5);

const fakeUser = {
  id: '42',
  username: 'PlayerOne',
  tag: 'PlayerOne#0001',
  toString: () => '<@42>',
  displayAvatarURL: ({ forceStatic }) =>
    forceStatic === false ? 'https://example.com/avatar.gif' : 'https://example.com/avatar.png',
  createdAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000), // 2 days old
};
const fakeMember = {
  user: fakeUser,
  displayName: 'PlayerOne',
  joinedAt: new Date('2026-08-05T10:00:00Z'),
};
const fakeGuild = {
  id: 'guild1',
  name: 'Heavenys',
  memberCount: 1284,
  iconURL: () => 'https://example.com/icon.png',
};

assert.equal(isNewAccount(fakeUser, 7), true);
assert.equal(avatarUrl(fakeUser), 'https://example.com/avatar.gif');

const welcomePayload = buildPresenceEmbed('welcome', fakeMember, fakeGuild, welcomeCfg);
assert.ok(welcomePayload.embeds[0].data.description.includes('Good luck and have fun'));
assert.equal(welcomePayload.embeds[0].data.thumbnail.url, 'https://example.com/avatar.gif');
assert.ok(
  welcomePayload.embeds[0].data.fields.some((f) => f.name.includes('New account')),
);
assert.equal(welcomePayload.components[0].components.length, 5);
assert.equal(
  welcomePayload.components[0].components[4].data.url,
  'https://tracker.gg/valorant',
);

const goodbyePayload = buildPresenceEmbed('goodbye', fakeMember, fakeGuild, welcomeCfg, {
  leftAt: Date.now(),
});
assert.ok(goodbyePayload.embeds[0].data.description.includes('has left the server'));
assert.ok(goodbyePayload.embeds[0].data.fields.some((f) => f.name === 'Left'));

// Persistence round-trip
const tmpDb = path.join(__dirname, '..', 'data', 'welcome-selfcheck.json');
const persisted = saveWelcomeConfig(welcomeCfg);
assert.equal(persisted.enabled, true);
assert.ok(fs.existsSync(DATA_PATH));

const svc = new WelcomeService({ on() {} }, welcomeCfg);
assert.equal(svc.enabled, true);
svc.setEnabled(false);
assert.equal(svc.config.enabled, false);
svc.setEnabled(true);
svc.setWelcomeMessage('Hello {member}');
assert.ok(svc.config.welcome.description.includes('Hello'));
svc.setButtonUrl('tracker', 'https://tracker.gg/valorant/profile/test', {
  label: 'Valorant Tracker',
  emoji: '🎮',
});
assert.ok(svc.config.buttons.some((b) => b.id === 'tracker'));

const raw = upsertButton([], 'rules', { label: 'Rules', channelId: '1', enabled: true });
assert.equal(raw[0].id, 'rules');

assert.ok(fs.existsSync(EXAMPLE_PATH));

// cleanup selfcheck noise from shared DB — restore defaults from example if we polluted
try {
  const example = JSON.parse(fs.readFileSync(EXAMPLE_PATH, 'utf8'));
  saveWelcomeConfig(example);
} catch {
  /* ignore */
}

if (fs.existsSync(tmpDb)) fs.unlinkSync(tmpDb);

console.log('selfcheck OK');
