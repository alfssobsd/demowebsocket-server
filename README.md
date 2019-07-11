# Demo Websocket Server (proof of concept)


Event (
  type: SUBSCRIBE, UNSUBSCRIBE, MESSAGE
  payload: Text
)

ChannelSubscribe (
    queue: <connection uniq name>
    createAt: timestamp - вермя когда когда была создана подписка 
)