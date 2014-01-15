#!/usr/bin/env bash
if [ $# -gt 0 && $1 = -r ];
	then git reset --hard;fi
git pull
./run.sh simulator