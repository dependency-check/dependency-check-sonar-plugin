const path = require("path");

module.exports = {
  // Define the entry points here. They MUST have the same name as the page_id
  // defined in src/main/java/org/sonarsource/plugins/example/web/MyPluginPageDefinition.java
  entry: {
    // Using React:
    report_page: ["./src/main/js/report_page/index.js"],
  },
  output: {
    // The entry point files MUST be shipped inside the final JAR's static/
    // directory.
    path: path.join(__dirname, "../target/classes/static"),
    filename: "[name].js"
  },
  resolve: {
    modules: [
      path.join(__dirname, "src/main/js"),
      "node_modules"
    ]
  },
  externals: {
    // React 16.8 ships with SonarQube, and should be re-used to avoid
    // collisions at runtime.
    react: "React",
    "react-dom": "ReactDOM",
    // Register the Sonar* globals as packages, to simplify importing.
    // See src/main/js/common/api.js for more information on what is exposed
    // in SonarRequest.
    "sonar-request": "SonarRequest",
  },
  module: {
    // Our example uses Babel to transpile our code.
    rules: [
      {
        test: /\.js$/,
        use: [
          "babel-loader",
        ],
        exclude: /(node_modules|bower_components)/,
      },
      {
        test: /\.css/,
        use: [
          { loader: 'style-loader'},
          { loader: 'css-loader'},
          { loader: 'postcss-loader',
            options: {
              postcssOptions: {
                ident: 'postcss',
                plugins: [
                  require('autoprefixer'),
                ]
              },
            }
          }
        ],
      }
    ]
  }
};
