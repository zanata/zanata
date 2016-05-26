## This is a module to build zanata frontend javascript projects

This module contains: User profile page, Glossary page, and Zanata side menu bar.

## To run in dev mode (need http://localhost:8080/zanata to run separately):

Navigate to frontend/src/main/web and run

```npm install```

to install all dependencies. To start

```npm start```

## To build it just run

```mvn install```

It will build and deploy to local maven repository a jar file containing the javascript bundle.
The jar file can be used directly under any servlet 3 compatible container and the bundle is accessible as static resources.
See [Servlet 3 static resources](http://www.webjars.org/documentation#servlet3).

The following Maven properties can be overridden on the command line with ```-Dkey=value```:

```
<node.version>v5.6.0</node.version>
<npm.version>3.6.0</npm.version>
<node.install.directory>${download.dir}/zanata-frontend/node-${node.version}-npm-${npm.version}</node.install.directory>
<npm.cli.script>${node.install.directory}/node/npm/bin/npm-cli.js</npm.cli.script>
```

By default it will try to install npm modules from npm registry (default cache TTL is 10 seconds).
If you activate profile ```-DnpmOffline``` the cache-min option will become 9999999 which means it will try to install npm modules from cache first.

## NPM shrinkwrap

Currently this module has been "shrinkwrapped" which means its npm module dependencies has been fixed to certain version. If you want to add or upgrade an individual version, you will need to consult [npm shrinkwrap documentation](https://docs.npmjs.com/cli/shrinkwrap#building-shrinkwrapped-packages) for detail instruction.

Since we use maven to copy our source to target/ then run npm from maven, you will need to run above commands under target/ then copy the new npm-shrinkwrap.json file back to src/.
