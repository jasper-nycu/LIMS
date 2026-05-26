// src/api/notificationSocket.ts
import { Client, type IMessage } from '@stomp/stompjs';

export type NotificationPayload = {
  senderId: string;
  title: string;
  desc: string;
  type: 'info' | 'success' | 'error' | 'warning';
  target: 'owners' | 'lab';
};

let client: Client | null = null;

// Derive WebSocket URL from the current page host so Vite proxy is used in dev
// and the real host is used in production.
function getWsUrl(): string {
  const proto = window.location.protocol === 'https:' ? 'wss' : 'ws';
  return `${proto}://${window.location.host}/ws`;
}

export function connectNotifications(
  sessionId: string,
  onMessage: (payload: NotificationPayload) => void
): () => void {
  client = new Client({
    brokerURL: getWsUrl(),
    reconnectDelay: 5000,
    onConnect: () => {
      client!.subscribe('/topic/notifications', (msg: IMessage) => {
        const payload: NotificationPayload = JSON.parse(msg.body);
        // Ignore messages sent by this same session (already shown locally)
        if (payload.senderId !== sessionId) {
          onMessage(payload);
        }
      });
    },
  });

  client.activate();
  return () => { client?.deactivate(); client = null; };
}

export function sendNotification(payload: NotificationPayload): void {
  if (client?.connected) {
    client.publish({
      destination: '/app/notify',
      body: JSON.stringify(payload),
    });
  }
}
