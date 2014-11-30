#!/bin/bash

# Run this script to upgrade from previously installed \BlueLaTeX instance

# Where the \BlueLaTeX binaries are installed
INSTALL_DIR=$install_dir

# Where the \BlueLaTeX configuration files are installed
CONF_DIR=$conf_dir

# Where the paper data will be stored
DATA_DIR=$data_dir

if [[ ! -e "$INSTALL_DIR" && ! (-e "$CONF_DIR" && -e "$DATA_DIR") ]]
then
  echo "No previous installation can be found. Please consider using 'install.sh' instead"
  exit 1
fi

if [[ -d "$INSTALL_DIR" ]]
then
  # If the installation directory already exists, check the diff between the old and new one
  diff -r bundle $INSTALL_DIR/bundle > /dev/null
  if [[ $? -ne 0 ]]
  then
    rm $INSTALL_DIR/bundle/*.jar
    cp bundle/*.jar $INSTALL_DIR/bundle/
  fi
  diff -r bin $INSTALL_DIR/bin > /dev/null
  if [[ $? -ne 0 ]]
  then
    rm $INSTALL_DIR/bin/*
    cp bin/* $INSTALL_DIR/bin/
  fi
else
  mkdir -p $INSTALL_DIR
  cp -r bin bundle $INSTALL_DIR
fi

diff -r conf $CONF_DIR > configuration.diff
if [[ $? -ne 0 ]]
then
  echo "Configuration has changed since last installed version. Check diff in 'configuration.diff' and take appropriate actions to finish upgrade"
else
  rm configuration.diff
fi

# If the data directory already exists, check the diff between the old and new one
diff -r data/classes $DATA_DIR/classes > classes.diff
if [[ $? -ne 0 ]]
then
  echo "LaTeX classes have changed since last installed version. Check diff in 'classes.diff' and take appropriate actions to finish upgrade"
else
  rm classes.diff
fi

diff -r data/designs $DATA_DIR/designs > designs.diff
if [[ $? -ne 0 ]]
then
  echo "CouchDB design shave changed since last installed version. Check diff in 'designs.diff' and take appropriate actions to finish upgrade"
else
  rm designs.diff
fi

diff -r data/templates $DATA_DIR/templates > templates.diff
if [[ $? -ne 0 ]]
then
  echo "Templates have changed since last installed version. Check diff in 'templates.diff' and take appropriate actions to finish upgrade"
else
  rm templates.diff
fi
