'use strict';

const fs = require('node:fs');
const path = require('node:path');
const {
  Client,
  Collection,
  GatewayIntentBits,
  Partials,
  Events,
} = require('discord.js');

const config = require('./config');
const { GameStore } = require('./services/GameStore');
const { GameManager } = require('./services/GameManager');
const { parseKda, parseScore, validateMatchScores } = require('./utils/score');

const client = new Client({
  intents: [
    GatewayIntentBits.Guilds,
    GatewayIntentBits.GuildVoiceStates,
    GatewayIntentBits.GuildMembers,
  ],
  partials: [Partials.Channel],
});

client.commands = new Collection();
const commandsPath = path.join(__dirname, 'commands');
for (const file of fs.readdirSync(commandsPath).filter((f) => f.endsWith('.js'))) {
  const command = require(path.join(commandsPath, file));
  if (command?.data?.name) {
    client.commands.set(command.data.name, command);
  }
}

const store = new GameStore();
const gameManager = new GameManager(client, store, config);
const ctx = { gameManager, store, config };

client.once(Events.ClientReady, (c) => {
  console.log(`Connecté en tant que ${c.user.tag}`);
  console.log(
    `Parties actives en mémoire : ${store.listActive().length} · cleanup=${config.voiceCleanupMode} · teamSize=${config.teamSize}`,
  );
});

client.on(Events.InteractionCreate, async (interaction) => {
  try {
    if (interaction.isChatInputCommand()) {
      const command = client.commands.get(interaction.commandName);
      if (!command) return;
      await command.execute(interaction, ctx);
      return;
    }

    if (interaction.isButton()) {
      const [ns, action, gameId] = interaction.customId.split(':');
      if (ns !== 'game' || !gameId) return;

      if (action === 'join') return gameManager.joinGame(interaction, gameId);
      if (action === 'leave') return gameManager.leaveGame(interaction, gameId);
      if (action === 'cancel') return gameManager.cancelGame(interaction, gameId);
      return;
    }

    if (interaction.isModalSubmit()) {
      const [ns, action, gameId] = interaction.customId.split(':');
      if (ns !== 'game' || action !== 'result' || !gameId) return;

      const game = store.get(gameId);
      const denied = gameManager.assertCanSubmitResult(
        interaction.member,
        interaction.user.id,
        game,
      );
      if (denied) {
        return interaction.reply({ content: denied, ephemeral: true });
      }

      const score1 = parseScore(interaction.fields.getTextInputValue('score1'));
      const score2 = parseScore(interaction.fields.getTextInputValue('score2'));
      const mvpName = interaction.fields.getTextInputValue('mvp_name').trim();
      const kda = parseKda(interaction.fields.getTextInputValue('mvp_kda'));

      if (score1 === null || score2 === null) {
        return interaction.reply({
          content: 'Scores invalides. Entrez des entiers entre 0 et 99.',
          ephemeral: true,
        });
      }
      const scoreErr = validateMatchScores(score1, score2);
      if (scoreErr) {
        return interaction.reply({ content: scoreErr, ephemeral: true });
      }
      if (!mvpName) {
        return interaction.reply({ content: 'Le pseudo du MVP est obligatoire.', ephemeral: true });
      }
      if (!kda) {
        return interaction.reply({
          content: 'K/D/A invalide. Format attendu : `24/12/5`.',
          ephemeral: true,
        });
      }

      await interaction.deferReply({ ephemeral: true });

      await gameManager.finishGame(interaction.guild, game, {
        score1,
        score2,
        mvp: { name: mvpName, ...kda },
        recordedBy: interaction.user.id,
        recordedAt: Date.now(),
      });

      return interaction.editReply({
        content:
          `Résultat enregistré : **${score1}–${score2}** · MVP **${mvpName}** (\`${kda.kills}/${kda.deaths}/${kda.assists}\`).` +
          `\nEmbed mis à jour · salons vocaux ${config.voiceCleanupMode === 'archive' ? 'archivés' : 'supprimés'}.`,
      });
    }
  } catch (err) {
    console.error('[interaction]', err);
    const payload = {
      content: 'Une erreur est survenue lors du traitement de l\'interaction.',
      ephemeral: true,
    };
    if (interaction.deferred || interaction.replied) {
      await interaction.followUp(payload).catch(() => null);
    } else {
      await interaction.reply(payload).catch(() => null);
    }
  }
});

client.login(config.token);
