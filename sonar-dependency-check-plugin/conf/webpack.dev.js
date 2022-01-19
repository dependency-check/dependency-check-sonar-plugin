const { merge } = require('webpack-merge');
const common = require("./webpack.common.js");
const webpack = require('webpack');

const PROXY_URL = process.env.PROXY_URL || 'http://localhost:9000';
const PROXY_CONTEXT_PATH = process.env.PROXY_CONTEXT_PATH || ''
const DEFAULT_PORT = process.env.PORT || 3000;


module.exports = merge(common, {
  devtool: 'eval',
  mode: 'development',
  output: {
    pathinfo: true,
    publicPath: PROXY_CONTEXT_PATH + "/static/dependencycheck/",
  },
  devServer: {
    port: DEFAULT_PORT,
    devMiddleware: {
      index: false, // specify to enable root proxying
    },
    proxy: {
      context: () => true,
      target: PROXY_URL,
      secure: false,
      changeOrigin: true,
    },
  },
  plugins: [new webpack.HotModuleReplacementPlugin()]
});
