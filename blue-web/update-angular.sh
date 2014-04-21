#! /bin/sh
if [ -n "$1" ]; then
  mkdir tmp
  curl https://raw.github.com/angular/code.angularjs.org/master/$1/angular-$1.zip -o tmp/angular.zip
  rm -fr web_client/lib/angular
  unzip tmp/angular.zip -d web_client/lib
  mv web_client/lib/angular-$1 web_client/lib/angular
  rm -fr web_client/lib/angular/docs
  mv web_client/lib/angular/angular-mocks.js test/lib/angular
  mv web_client/lib/angular/angular-scenario.js test/lib/angular
  cp web_client/lib/angular/version.txt test/lib/angular

else
  echo "Usage: update-angular <version>"
fi
