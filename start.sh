#!/usr/bin/env bash

set -e

if [ ! -n "$1" ]; then
echo "you should input service name!"
exit 1
fi

basedir=`cd $(dirname $0); pwd -P`
echo $basedir

mvn clean install -Dmaven.test.skip=true

if [ -d binance-mgs-application ]; then
  echo "binance-mgs-application is not allowed."
  exit 1
fi

if [ -d $1 ]; then
  cd $1 && mvn clean package -Dmaven.test.skip=true -U && cd ${basedir}
elif [ -d binance-mgs-application/$1 ]; then
  cd binance-mgs-application/$1 && mvn clean package -Dmaven.test.skip=true -U && cd ${basedir}
else
  echo "CANNOT find service $1"
  exit 1
fi
