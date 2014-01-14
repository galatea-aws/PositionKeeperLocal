#!/usr/bin/env bash
git reset --hard
git pull
./run.sh clean
./run.sh querytester CountTradesByAccountTester
./run.sh querytester SumPositionByAccountAndProductTester