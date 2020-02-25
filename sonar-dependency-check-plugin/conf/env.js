/*
 * Copyright (C) 2017-2017 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
// Grab NODE_ENV and REACT_APP_* environment variables and prepare them to be
// injected into the application via DefinePlugin in Webpack configuration.

const REACT_APP = /^REACT_APP_/i;

function getClientEnvironment() {
  return Object.keys(process.env).filter(key => REACT_APP.test(key)).reduce((env, key) => {
    env['process.env.' + key] = JSON.stringify(process.env[key]);
    return env;
  }, {
    // Useful for determining whether we’re running in production mode.
    // Most importantly, it switches React into the correct mode.
    'process.env.NODE_ENV': JSON.stringify(process.env.NODE_ENV || 'development')
  });
}

module.exports = getClientEnvironment;
