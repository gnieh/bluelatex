#!/bin/sh

/usr/bin/jsvc \
  -wait 10 \
  -java-home /usr/lib/jvm/default-java \
  -cp $install_dir/bin/blue-launcher.jar \
  -user $blue_user \
  -pidfile /var/run/bluelatex.pid \
  -outfile $log_dir/bluelatex.out \
  -errfile $log_dir/bluelatex.err \
  -stop \
  org.gnieh.blue.launcher.Main
