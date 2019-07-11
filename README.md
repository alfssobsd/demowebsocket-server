# Demo Websocket Server (proof of concept)


## Arch overview
![Arch Oveview](/docs/overview.jpg)

## Describe main processes

### Send message
![Send message](/docs/send-message.jpg)

### Subscribe to channel 
![Subscribe to channel](/docs/subscribe-to-channel.jpg)


## Avalible websocket request (for client) 

Action | Example message in json
------------ | -------------
subscribe to the channel room1| ``` { "type": "SUBSCRIBE", "payload": "room1"}```
unsubscribe from the channel  room1| ``` { "type": "UNSUBSCRIBE", "payload": "room1"}```
answer on ping message| ``` { "type": "PONG", "payload": "PONG"}```

## Avalible websocket response (for client)

Action | Example message in json
------------ | -------------
Subscribe result | ``` { "type": "RESPONSE_SUBSCRIBE", "payload": "SUBSCRIBE_OK(room1)", code: 200}```
Subscribe result (access denied)| ``` { "type": "RESPONSE_SUBSCRIBE", "payload": "SUBSCRIBE_ACCESS_DENIED(room1)", code: 403}```
Unsubscrube result | ``` { "type": "RESPONSE_UNSUBSCRIBE", "payload": "UNSUBSCRIBE_OK(room1)", code: 200}```
Receiving data | ``` { "type": "DATA", "payload": "message text or json", code: 200}```
Disconnect | ``` { "type": "DISCONNECT", "payload": "DISCONNECT", code: 200}```
Ping (check connection, you should answer PONG) | ``` { "type": "PING", "payload": "PING", code: 200}```


# Build and try

## Start redis
```
docker-compose up -d
```

## Start demo_ws_push_executor (port 8001)
```
./gradlew :demo_ws_push_executor:bootRun
```

## Start demo_ws_public_executor (port 8000)
```
./gradlew :demo_ws_public_executor:bootRun
```


## Start demo push client for send message 
run `net/alfss/demowsclient/DemoPushClientMain.kt`

## Start demo websocket client 
run `net/alfss/demowsclient/DemoWsClientMain.kt`