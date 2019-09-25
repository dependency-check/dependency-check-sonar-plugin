# Releasing

## Deploy Release to GitHub and Bintray

Deployment to GitHub and Bintray should be done automatic.
```bash
mvn release:prepare release:perform
```

## Deploy SNAPSHOT to GitHub
Deploy SNAPSHOTs to Bintray is not possible. Therefore we skip bintray, when we deploy manuell to GitHub.
```bash
mvn clean deploy -Pskip-bintray
```
