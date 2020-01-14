#!/bin/bash

set -euo pipefail

function configureTravis {
  mkdir -p ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v57 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install                                                                                                                                    
}

function prepareReleaseProperties {
  ./release/createReleaseProperties.sh
  # Install GPG secrets so that we can sign during the release
  echo $GPG_SECRET_KEYS | base64 --decode | gpg --import
  echo $GPG_OWNERTRUST | base64 --decode | gpg --import-ownertrust
}
configureTravis


if [[ "${TRAVIS_BRANCH}" == "master" ]] && [[ "$TRAVIS_PULL_REQUEST" == "false" ]]; then
   echo "======== Build, deploy and analyze master"

   # Analyze with SNAPSHOT version as long as SonarQube does not correctly handle
   # purge of release data
   CURRENT_VERSION=`maven_expression "project.version"`

   . set_maven_build_version $TRAVIS_BUILD_NUMBER

   prepareReleaseProperties

   export MAVEN_OPTS="-Xmx1536m -Xms128m"
   mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent deploy sonar:sonar \
      --settings release/mvnsettings.xml \
      -Pcoverage,release \
      -Dmaven.test.redirectTestOutputToFile=false \
      -Dsonar.projectVersion=$CURRENT_VERSION \
      -Dsonar.projectKey=societe-generale_sonar-openapi \
      -Dsonar.analysis.buildNumber=$TRAVIS_BUILD_NUMBER \
      -Dsonar.analysis.pipeline=$TRAVIS_BUILD_NUMBER \
      -Dsonar.analysis.sha1=$TRAVIS_COMMIT  \
      -Dsonar.analysis.repository=$TRAVIS_REPO_SLUG \
      -B -e -V $*

elif [[ "${TRAVIS_BRANCH}" == "branch-"* ]] && [[ "$TRAVIS_PULL_REQUEST" == "false" ]]; then
  # analyze maintenance branches as long-living branches

  export MAVEN_OPTS="-Xmx1536m -Xms128m"

  # get current version from pom
  CURRENT_VERSION=`maven_expression "project.version"`

  mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent install sonar:sonar \
      -Pcoverage \
      -Dmaven.test.redirectTestOutputToFile=false \
      -Dsonar.branch.name=$TRAVIS_BRANCH \
      -Dsonar.projectKey=societe-generale_sonar-openapi \
      -Dsonar.analysis.buildNumber=$TRAVIS_BUILD_NUMBER \
      -Dsonar.analysis.pipeline=$TRAVIS_BUILD_NUMBER \
      -Dsonar.analysis.sha1=$TRAVIS_COMMIT  \
      -Dsonar.analysis.repository=$TRAVIS_REPO_SLUG \
      -B -e -V $*

elif [[ "$TRAVIS_PULL_REQUEST" != "false" ]] && [[ -n "${GITHUB_TOKEN:-}" ]]; then
  echo '======= Build and analyze pull request'

  # Do not deploy a SNAPSHOT version but the release version related to this build and PR
  . set_maven_build_version $TRAVIS_BUILD_NUMBER

  # No need for Maven phase "install" as the generated JAR files do not need to be installed
  # in Maven local repository. Phase "verify" is enough.

  export MAVEN_OPTS="-Xmx1G -Xms128m"
  mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent verify sonar:sonar \
      -Pcoverage \
      -Dmaven.test.redirectTestOutputToFile=false \
      -Dsonar.projectKey=societe-generale_sonar-openapi \
      -Dsonar.analysis.buildNumber=$TRAVIS_BUILD_NUMBER \
      -Dsonar.analysis.pipeline=$TRAVIS_BUILD_NUMBER \
      -Dsonar.analysis.sha1=$TRAVIS_PULL_REQUEST_SHA  \
      -Dsonar.analysis.repository=$TRAVIS_REPO_SLUG \
      -Dsonar.analysis.prNumber=$TRAVIS_PULL_REQUEST \
      -Dsonar.pullrequest.branch=$TRAVIS_PULL_REQUEST_BRANCH \
      -Dsonar.pullrequest.base=$TRAVIS_BRANCH \
      -Dsonar.pullrequest.key=$TRAVIS_PULL_REQUEST \
      -Dsonar.pullrequest.provider=github \
      -Dsonar.pullrequest.github.repository=$TRAVIS_REPO_SLUG \
      -B -e -V $*

elif [[ "$TRAVIS_BRANCH" == "feature/long/"* ]] && [[ "$TRAVIS_PULL_REQUEST" == "false" ]]; then
  echo '======= Build and analyze long lived feature branch'
    
  mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent verify sonar:sonar \
    -Pcoverage \
    -Dmaven.test.redirectTestOutputToFile=false \
    -Dsonar.projectKey=societe-generale_sonar-openapi \
    -Dsonar.branch.name=$TRAVIS_BRANCH \
    -Dsonar.analysis.buildNumber=$TRAVIS_BUILD_NUMBER \
    -Dsonar.analysis.pipeline=$TRAVIS_BUILD_NUMBER \
    -Dsonar.analysis.sha1=$TRAVIS_COMMIT  \
    -Dsonar.analysis.repository=$TRAVIS_REPO_SLUG \
    -B -e -V $*    
        
else
  echo '======= Build, no analysis, no deploy'

  # No need for Maven phase "install" as the generated JAR files do not need to be installed
  # in Maven local repository. Phase "verify" is enough.

  mvn clean verify \
      -Dmaven.test.redirectTestOutputToFile=false \
      -B -e -V $*
fi
