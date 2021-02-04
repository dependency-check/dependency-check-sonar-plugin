# Releasing

## Deploy Release to GitHub

Deployment to GitHub should be done automatic.
```bash
mvn release:prepare release:perform
```

## Deploy SNAPSHOT to GitHub
```bash
mvn clean deploy
```
