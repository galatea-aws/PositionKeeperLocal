#!/usr/bin/env bash
git reset --hard
git pull
./run.sh clean
./run.sh querytester SumPositionByAccountAndProductTester