#!/usr/bin/env bash
export JAVA_HOME=`/usr/libexec/java_home -v 1.8`
export PATH=JAVA_HOME/bin:$PATH

read -p "Are you sure you want to release (Y/N)? "
if ( [ "$REPLY" == "Y" ] ) then

  mvn clean
  mvn release:clean release:prepare release:perform -Prelease -X -e | tee release.log
  mvn github-release:release

else
  echo -e "Exit without deploy"
fi