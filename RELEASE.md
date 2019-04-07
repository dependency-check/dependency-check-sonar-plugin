# Releasing

## Deploy Release to GitHub and Bintray

Deployment to GitHub and Bintray should be done automatic.
```
mvn release:prepare release:perform
```


## Deploy SNAPSHOT to GitHub
Deploy SNAPSHOTs to Bintray is not possible. Therefore we skip bintray, when we deploy manuell to GitHub.
```
mvn clean deploy -Pskip-bintray
```
