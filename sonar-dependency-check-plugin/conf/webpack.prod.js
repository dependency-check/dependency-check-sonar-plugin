const { merge } = require('webpack-merge');
const common = require('./webpack.common.js');

module.exports = merge(common, {

  mode: 'production',
  // Don't attempt to continue if there are any errors.
  bail: true,
  optimization: {
    minimize: true
  },
});
