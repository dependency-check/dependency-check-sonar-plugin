FROM mcr.microsoft.com/dotnet/sdk:6.0

ENV NODEREPO=node_16.x \
    DEPENDENCY_CHECK=6.5.3 \
    PATH="$PATH:/root/.dotnet/tools"

RUN set -x && \
    /usr/bin/apt-get update && \
    # Install Java 8 and other tools
    DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-11-jre-headless unzip wget lsb-release gnupg apt-transport-https && \
    # Install Sonarqube-Scanner
    dotnet tool install --global dotnet-sonarscanner && \
    # Install dependency-check
    /usr/bin/wget -q -O /opt/dependency-check-${DEPENDENCY_CHECK}-release.zip https://github.com/jeremylong/DependencyCheck/releases/download/v${DEPENDENCY_CHECK}/dependency-check-${DEPENDENCY_CHECK}-release.zip && \
    /usr/bin/wget -q -O /opt/dependency-check-${DEPENDENCY_CHECK}-release.zip.asc https://github.com/jeremylong/DependencyCheck/releases/download/v${DEPENDENCY_CHECK}/dependency-check-${DEPENDENCY_CHECK}-release.zip.asc && \
    /usr/bin/gpg --batch --keyserver keyserver.ubuntu.com --recv-keys 259A55407DD6C00299E6607EFFDE55BE73A2D1ED && \
    /usr/bin/gpg --batch --verify /opt/dependency-check-${DEPENDENCY_CHECK}-release.zip.asc /opt/dependency-check-${DEPENDENCY_CHECK}-release.zip && \
    /usr/bin/unzip /opt/dependency-check-${DEPENDENCY_CHECK}-release.zip -d /opt && \
    /bin/mkdir /opt/dependency-check/data && \
    /bin/chmod g+w -R /opt/dependency-check/data && \
    /bin/ln -s /opt/dependency-check/bin/dependency-check.sh /usr/bin/dependency-check.sh && \
    /bin/rm -v /opt/dependency-check-${DEPENDENCY_CHECK}-release.zip.asc /opt/dependency-check-${DEPENDENCY_CHECK}-release.zip && \
    # Install Node for Sonarqube-Scanner
    # Docu Way, but without a potential dangerous script.
    # https://nodejs.org/en/download/package-manager/#debian-and-ubuntu-based-linux-distributions
    /usr/bin/wget -qO- https://deb.nodesource.com/gpgkey/nodesource.gpg.key | apt-key add - && \
    echo "deb https://deb.nodesource.com/${NODEREPO} $(lsb_release -c -s) main" > /etc/apt/sources.list.d/nodesource.list && \
    echo "deb-src https://deb.nodesource.com/${NODEREPO} $(lsb_release -c -s) main" >> /etc/apt/sources.list.d/nodesource.list && \
    /usr/bin/apt-get update && \
    DEBIAN_FRONTEND=noninteractive /usr/bin/apt-get install -y nodejs && \
    # Cleanup
    /usr/bin/apt-get clean && \
    /bin/rm -rf /var/lib/apt/lists/*

WORKDIR /app

ARG sonarHost
ARG sonarLogin
ARG sonarPassword

COPY *.sln ./
COPY ExampleApp1 ./ExampleApp1
COPY ExampleApp2 ./ExampleApp2

RUN /usr/bin/dependency-check.sh -f JSON -f HTML -s . -o .
RUN dotnet sonarscanner begin /k:exampleCSproj \
    /d:sonar.host.url=${sonarHost} /d:sonar.login=${sonarLogin} /d:sonar.password=${sonarPassword} \
    /d:sonar.dependencyCheck.jsonReportPath="/app/dependency-check-report.json" \
    /d:sonar.dependencyCheck.htmlReportPath="/app/dependency-check-report.html" && \
    dotnet build && \
    dotnet sonarscanner end /d:sonar.login=${sonarLogin} /d:sonar.password=${sonarPassword}
