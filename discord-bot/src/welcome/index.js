'use strict';

module.exports = {
  WelcomeService: require('./WelcomeService').WelcomeService,
  loadWelcomeConfig: require('./configLoader').loadWelcomeConfig,
  saveWelcomeConfig: require('./configLoader').saveWelcomeConfig,
  buildPresenceEmbed: require('./embeds').buildPresenceEmbed,
};
