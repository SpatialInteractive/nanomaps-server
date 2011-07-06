#!/bin/bash
cd $(dirname $0)/..

git submodule init
git submodule update

# Recursive update
git submodule foreach git submodule init
git submodule foreach git submodule update
