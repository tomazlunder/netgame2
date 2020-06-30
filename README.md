# netgame2

A simple pong game in java, Libgdx framework with multiplayer support.

Features:
  - Multiplayer with UDP protocol.
  - A pong game with a simple AI for solo play.
  - A gameserver/loby where clients clients get matched and play (authoritative).
  - Client only authoritative over its own paddle, possible serverside checks for valid moves. 
  
TODO:
  - A loby/nat punching server for clients to find outher clients to play against, with one of the clients running the game thread.
