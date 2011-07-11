#!/bin/bash
td=$(dirname $0)/..
moddir=$1
moddir=${moddir%%/}

if [ -z "$moddir" ] || ! [ -d "$td/$moddir" ]; then
	echo "Expected module directory"
	exit 1
fi

pushd $td/$moddir
git checkout master
git pull
popd

pushd $td
git add $td/$moddir
git commit -m "Pull new version of $moddir"
git status

