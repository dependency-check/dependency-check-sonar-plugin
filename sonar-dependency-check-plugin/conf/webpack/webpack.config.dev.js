/*
 * Copy from https://github.com/SonarSource/sonar-custom-plugin-example/blob/7.x/conf/webpack/webpack.config.dev.js
 *
 * Copyright (C) 2017-2017 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
const webpack = require("webpack");
const config = require("./webpack.config");

config.devtool = "eval";

config.output.publicPath = "/static/dependencycheck/";

config.output.pathinfo = true;

Object.keys(config.entry).forEach((key) => {
  config.entry[key].unshift(require.resolve("react-dev-utils/webpackHotDevClient"));
});

config.plugins = [new webpack.HotModuleReplacementPlugin()];

module.exports = config;
