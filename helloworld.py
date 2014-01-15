#!/usr/bin/python
import paramiko
hostname = '172.31.22.149' 
port = 22
username = 'voltdb' 
password = 'voltdb'
print ('helloworld')
if __name__ == "__main__": 
    paramiko.util.log_to_file('paramiko.log')
    s = paramiko.SSHClient()
    s.load_system_host_keys()
    s.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    s.connect(hostname, port, username, password) 
    stdin, stdout, stderr = s.exec_command('getconf PATH') 
    print stdout.read()
    s.close()
