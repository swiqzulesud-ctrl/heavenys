'use strict';

/**
 * Lightweight sanity checks (no Discord connection).
 * Run: node src/selfcheck.js
 */

const assert = require('node:assert/strict');
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
assert.equal(
  channelUrl('111', '222'),
  'https://discord.com/channels/111/222',
);

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

const embed = buildGameEmbed(sampleGame);
assert.ok(embed.data.title.includes('Partie'));

const rows = buildGameComponents(sampleGame);
assert.equal(rows.length, 2);
assert.equal(rows[1].components.length, 2);
assert.equal(rows[1].components[0].data.custom_id, `game:voice:${sampleGame.id}`);
assert.equal(rows[1].components[1].data.custom_id, `game:info:${sampleGame.id}`);

const withInvite = { ...sampleGame, inviteUrl: 'https://example.com/join' };
const linkRows = buildGameComponents(withInvite);
assert.equal(linkRows[1].components[1].data.style, 5); // ButtonStyle.Link
assert.equal(linkRows[1].components[1].data.url, 'https://example.com/join');

const voiceRow = buildVoiceLinkRow('guild1', 'vc1');
assert.equal(voiceRow.components[0].data.url, 'https://discord.com/channels/guild1/vc1');

const store = new GameStore();
const id = 'test-game-selfcheck';
store.upsert({
  id,
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
assert.ok(store.get(id));
store.delete(id);

// Alternation simulation
const team1 = [];
const team2 = [];
const teamSize = 5;
for (let i = 0; i < 10; i++) {
  let team = i % 2 === 0 ? 1 : 2;
  if (team === 1 && team1.length >= teamSize) team = 2;
  if (team === 2 && team2.length >= teamSize) team = 1;
  (team === 1 ? team1 : team2).push(i);
}
assert.equal(team1.length, 5);
assert.equal(team2.length, 5);
assert.deepEqual(team1, [0, 2, 4, 6, 8]);
assert.deepEqual(team2, [1, 3, 5, 7, 9]);

console.log('selfcheck OK');
