#!/usr/bin/env bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
export PATH=JAVA_HOME/bin:$PATH

read -p "Are you sure you want to release? (Y/N)? "
if ( [ "$REPLY" == "Y" ] ) then
  read -p "Specify release version number to release (i.e. 1.0.0): "
  mvn clean
  mvn release:clean

  mvn versions:set -DnewVersion=$REPLY
  mvn package
  #git commit -m "Releasing $REPLY"
  #git push
  mvn github-release:release
  mvn versions:revert

  mvn release:clean release:prepare release:perform -Prelease -X -e | tee release.log

else
  echo -e "Exit without release"
fi