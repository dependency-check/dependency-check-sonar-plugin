/*
 * Copy from https://github.com/SonarSource/sonar-custom-plugin-example/blob/7.x/conf/webpack/webpack.config.prod.js
 *
 * Copyright (C) 2017-2017 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
const webpack = require('webpack');
const config = require('./webpack.config');
const getClientEnvironment = require('../env');

// Get environment variables to inject into our app.
const env = getClientEnvironment();

// Assert this just to be safe.
// Development builds of React are slow and not intended for production.
if (env['process.env.NODE_ENV'] !== '"production"') {
  throw new Error('Production builds must have NODE_ENV=production.');
}

// Don't attempt to continue if there are any errors.
config.bail = true;

config.mode = 'production';

config.plugins = [
  // Makes some environment variables available to the JS code, for example:
  // if (process.env.NODE_ENV === 'production') { ... }. See `./env.js`.
  // It is absolutely essential that NODE_ENV was set to production here.
  // Otherwise React will be compiled in the very slow development mode.
  new webpack.DefinePlugin(env),
];

config.optimization = {
  minimize: true
};

module.exports = config;
