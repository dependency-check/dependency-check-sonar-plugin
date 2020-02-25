/*
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

const noUglify = process.argv.some(arg => arg.indexOf('--no-uglify') > -1);

// Don't attempt to continue if there are any errors.
config.bail = true;

config.plugins = [
  // Makes some environment variables available to the JS code, for example:
  // if (process.env.NODE_ENV === 'production') { ... }. See `./env.js`.
  // It is absolutely essential that NODE_ENV was set to production here.
  // Otherwise React will be compiled in the very slow development mode.
  new webpack.DefinePlugin(env),

  // This helps ensure the builds are consistent if source hasn't changed:
  new webpack.optimize.OccurrenceOrderPlugin(),

  // Try to dedupe duplicated modules, if any:
  new webpack.optimize.DedupePlugin()
];

if (!noUglify) {
  config.plugins.push(
    new webpack.optimize.UglifyJsPlugin({
      compress: {
        screw_ie8: true, // React doesn't support IE8
        warnings: false
      },
      mangle: {
        screw_ie8: true
      },
      output: {
        comments: false,
        screw_ie8: true
      }
    })
  );
}

module.exports = config;
