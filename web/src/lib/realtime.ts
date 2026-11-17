import { Client } from "@stomp/stompjs";
import { useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { api } from "./api";
import { apiUrl } from "./config";
import { getTokens } from "./tokens";

/** STOMP endpoint: the api URL with the ws scheme, e.g. ws://localhost:8080/api/ws */
const wsUrl = `${apiUrl.replace(/^http/, "ws")}/ws`;

/**
 * Live updates: the api pushes the query-key prefixes that changed (see its ChangeBroadcaster) and we
 * invalidate them, so open lists refetch when anyone changes data. Reconnects automatically and refetches
 * everything after a reconnect, since events may have been missed.
 */
export function useRealtime() {
  const qc = useQueryClient();

  useEffect(() => {
    let connectedBefore = false;
    const client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 5000,
      beforeConnect: async () => {
        // an expired access token would be refused: any api call refreshes it first (see lib/api.ts)
        await api("/auth/me").catch(() => undefined);
        client.connectHeaders = { Authorization: `Bearer ${getTokens()?.accessToken ?? ""}` };
      },
      onConnect: () => {
        if (connectedBefore) qc.invalidateQueries();
        connectedBefore = true;
        client.subscribe("/topic/changes", (message) => {
          const { resources } = JSON.parse(message.body) as { resources: string[] };
          for (const key of resources) qc.invalidateQueries({ queryKey: [key] });
        });
      },
    });
    client.activate();
    return () => {
      void client.deactivate();
    };
  }, [qc]);
}
