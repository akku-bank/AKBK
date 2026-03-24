const fs = require('fs');
const path = require('path');

const { expo } = require('./app.json');

const config = {
  ...expo,
  android: expo.android
    ? {
        ...expo.android,
      }
    : undefined,
};

if (config.android) {
  const googleServicesPath = path.join(__dirname, 'google-services.json');

  if (!fs.existsSync(googleServicesPath)) {
    delete config.android.googleServicesFile;
  }
}

module.exports = {
  expo: config,
};