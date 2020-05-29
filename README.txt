# SDIS-FEUP

JAVA SE VERSION : 8.0

TO COMPILE

-> javac *.java

TO RUN

1. Start RMI 
-> rmiregistry

2. Create first Peer
-> java Peer <access_point> <address> <port> 
 e.x: Peer AP1 127.0.0.1 8000 

3. Create Peer(s)
-> java Peer <access_point> <address> <port> <active_peer_address> <active_peer_port>
 e.x: Peer AP1 127.0.0.2 8082 127.0.0.1 8000  

4. Start a Protocol

 ->Backup Protocol (To backup a file)
   - Client <peer_ap> BACKUP <file_path> [<desired_replication_degree>]
     e.x: Client AP1 BACKUP test.txt 2

 ->Delete Protocol (To delete a file)
   - Client <peer_ap> DELETE <file_path>
     e.x: Client AP1 DELETE test.txt

 ->Restore Protocol (To restore a file)
   - Client <peer_ap> RESTORE <file_path>
     e.x: Client AP1 RESTORE test.txt

 ->Reclaim Protocol (To reclaim space)
   - Client <peer_ap> RECLAIM <max_disk_space>
     e.x: Client AP1 RECLAIM 100000
