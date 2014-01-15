#!/usr/bin/env bash
. serverconfig.properties
SERVER_SH = "$VOLTDB_BASE/examples/PositionKeeper/server.sh"
echo 'start voltdb on 172.31.22.149'
ssh voltdb@172.31.22.149 bash < $SERVER_SH
echo 'start voltdb on 172.31.22.150'
ssh voltdb@172.31.22.150 bash < $SERVER_SH