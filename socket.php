<?php
set_time_limit(0);
$host = '0.0.0.0';
$port = 8080;

$socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
socket_set_option($socket, SOL_SOCKET, SO_REUSEADDR, 1);
socket_bind($socket, $host, $port);
socket_listen($socket);
socket_set_nonblock($socket);

$clients = [$socket];
echo "Server started on $host:$port\n";

while (true) {
    $read = $clients;
    $write = $except = null;
    
    if (socket_select($read, $write, $except, 0) < 1)
        continue;
        
    // Новое подключение
    if (in_array($socket, $read)) {
        $newClient = socket_accept($socket);
        $clients[] = $newClient;
        $key = array_search($socket, $read);
        unset($read[$key]);
    }
    
    // Обработка входящих данных
    foreach ($read as $client) {
        $data = socket_read($client, 1024);
        
        if ($data === false || strlen($data) === 0) {
            // Клиент отключился
            $key = array_search($client, $clients);
            socket_close($client);
            unset($clients[$key]);
            continue;
        }
        
        // Рассылка всем другим клиентам
        foreach ($clients as $otherClient) {
            if ($otherClient != $socket && $otherClient != $client) {
                socket_write($otherClient, $data, strlen($data));
            }
        }
    }
}

socket_close($socket);
?>