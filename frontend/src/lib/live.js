import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import { wsUrl } from './api.js';

/**
 * Live seat updates for one show. Calls onUpdate({ seatId, status }) for every hold, release, sale or
 * expired hold, from any visitor. Reconnects by itself; onReconnect lets the page reload the seat map,
 * because updates sent while disconnected are not replayed.
 *
 * @returns {'connecting' | 'live' | 'offline'}
 */
export function useSeatUpdates(showId, onUpdate, onReconnect) {
  const [state, setState] = useState('connecting');
  // Keep the newest callbacks without reconnecting every render.
  const handlers = useRef({ onUpdate, onReconnect });
  handlers.current = { onUpdate, onReconnect };

  useEffect(() => {
    let connectedBefore = false;
    const client = new Client({
      brokerURL: wsUrl(),
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setState('live');
        client.subscribe(`/topic/shows/${showId}/seats`, (message) => {
          handlers.current.onUpdate(JSON.parse(message.body));
        });
        if (connectedBefore) handlers.current.onReconnect?.();
        connectedBefore = true;
      },
      onWebSocketClose: () => setState('offline'),
      onStompError: () => setState('offline'),
    });
    client.activate();
    return () => {
      client.deactivate();
    };
  }, [showId]);

  return state;
}
