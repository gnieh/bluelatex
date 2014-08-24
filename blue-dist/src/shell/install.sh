#!/bin/bash

# Run this to install \BlueLaTeX distribution.

# Where the \BlueLaTeX binaries are installed
INSTALL_DIR=$install_dir

# Where the \BlueLaTeX configuration files are installed
CONF_DIR=$conf_dir

# Where the paper data will be stored
DATA_DIR=$data_dir

# Where the log files are created
LOG_DIR=$log_dir

# The user under which \BlueLaTeX will run
BLUE_USER=$blue_user

# Create directories
mkdir -p $INSTALL_DIR
mkdir -p $CONF_DIR
mkdir -p $DATA_DIR
mkdir -p $LOG_DIR

# Copy stuffs
cp -r bin bundle $INSTALL_DIR
cp -r conf/* $CONF_DIR
cp -r data/* $DATA_DIR

# Create the user if needed
id -u $BLUE_USER &>/dev/null
if [ $? -ne 0 ]
then
  adduser --system --group --disabled-password --disabled-login --no-create-home $BLUE_USER
fi

# Set correct ownership
chown -R $BLUE_USER:$BLUE_USER $DATA_DIR $LOG_DIR

echo "\\BlueLaTeX was successfully installed"
