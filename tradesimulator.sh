#!/usr/bin/env bash
if[$1 = -r];
	then git reset --hard
else;fi
git pull
./run.sh simulator