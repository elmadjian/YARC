#!/usr/bin/python

import socket

port = 11110

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.bind((socket.gethostname(), port))
sock.listen(1)

client, addr = sock.accept()
print("Accepting connection from:", addr)
while True:
    data = client.recv(2048)
    if not data:
        continue
    print("received data:", data)

client.close()