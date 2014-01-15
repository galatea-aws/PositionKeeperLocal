#!/usr/bin/env bash
echo 'start voltdb on 172.31.22.149'
ssh voltdb@172.31.22.149 bash < /path/to/local/script.sh
echo 'start voltdb on 172.31.22.150'
ssh voltdb@172.31.22.150 bash < /path/to/local/script.sh