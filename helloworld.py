#!/usr/bin/python
import paramiko
from pyjavaproperties import Properties
hostname = '172.31.22.149' 
port = 22
username = 'voltdb' 
password = 'voltdb'
print ('helloworld')
if __name__ == "__main__":  
    paramiko.util.log_to_file('paramiko.log')
    p = Properties()
    p.load(open('serverconfig.properties'))
    hostname = p['servers']
    s = paramiko.SSHClient()
    s.load_system_host_keys()
    s.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    s.connect(hostname, port, username, password) 
    s.exec_command('cd /home/voltdb/voltdb-3.5.0.1/examples/Positionkeeper; ./server.sh') 
    s.close()
