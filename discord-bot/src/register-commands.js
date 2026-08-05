'use strict';

const { REST, Routes } = require('discord.js');
const fs = require('node:fs');
const path = require('node:path');

// Load config without starting the bot — still needs env vars.
const config = require('./config');

const commandsDir = path.join(__dirname, 'commands');
const commandBodies = [];

for (const file of fs.readdirSync(commandsDir).filter((f) => f.endsWith('.js'))) {
  const cmd = require(path.join(commandsDir, file));
  if (cmd?.data) commandBodies.push(cmd.data.toJSON());
}

const rest = new REST({ version: '10' }).setToken(config.token);

(async () => {
  try {
    console.log(`Enregistrement de ${commandBodies.length} commande(s) slash…`);
    await rest.put(Routes.applicationGuildCommands(config.clientId, config.guildId), {
      body: commandBodies,
    });
    console.log('Commandes enregistrées sur le serveur', config.guildId);
  } catch (err) {
    console.error('Échec de l\'enregistrement des commandes:', err);
    process.exitCode = 1;
  }
})();
