'use strict';

module.exports = {
  WelcomeService: require('./WelcomeService').WelcomeService,
  loadWelcomeConfig: require('./configLoader').loadWelcomeConfig,
  buildPresenceEmbed: require('./embeds').buildPresenceEmbed,
};
