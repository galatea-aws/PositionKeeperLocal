#!/usr/bin/python
import paramiko
import time
from subprocess import call
from pyjavaproperties import Properties
if __name__ == "__main__":  
    paramiko.util.log_to_file('paramiko.log')
    p = Properties()
    p.load(open('serverconfig.properties'))
    s = paramiko.SSHClient()
    s.load_system_host_keys()
    s.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    hostlist = p['servers'].split(',')
    port = 22
    username = 'voltdb' 
    password = 'voltdb'
    for hostname in hostlist:
        print(hostname)
        s.connect(hostname, port, username, password) 
        s.exec_command('cd /home/voltdb/voltdb-3.5.0.1/examples/Positionkeeper; ./server.sh')
	print('waiting for server start')
	time.sleep(10)
	subprocess.call(['/home/voltdb/voltdb-3.5.0.1/examples/Positionkeeper/tradesimulator.sh'])
	subprocess.call(['/home/voltdb/voltdb-3.5.0.1/examples/Positionkeeper/querytester.sh'])
    s.close()
