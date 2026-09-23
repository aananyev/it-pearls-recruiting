#!/usr/bin/env python3
"""
HTTP CONNECT proxy that forwards to an upstream authenticated proxy.
Listens on localhost:8119, forwards CONNECT requests to 209.46.2.183:8000
with Proxy-Authorization header.
"""

import asyncio
import base64
import logging
import sys

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s %(levelname)s %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)
log = logging.getLogger(__name__)

UPSTREAM_HOST = "209.46.2.183"
UPSTREAM_PORT = 8000
UPSTREAM_USER = "tWQrfq"
UPSTREAM_PASS = "YtJRww"
LISTEN_HOST = "0.0.0.0"
LISTEN_PORT = 8119

AUTH_HEADER = base64.b64encode(f"{UPSTREAM_USER}:{UPSTREAM_PASS}".encode()).decode()


async def pipe(reader: asyncio.StreamReader, writer: asyncio.StreamWriter):
    try:
        while not reader.at_eof():
            data = await reader.read(8192)
            if not data:
                break
            writer.write(data)
            await writer.drain()
    except (ConnectionError, asyncio.CancelledError, OSError):
        pass
    finally:
        writer.close()
        try:
            await writer.wait_closed()
        except Exception:
            pass


async def handle_client(client_reader: asyncio.StreamReader, client_writer: asyncio.StreamWriter):
    peername = client_writer.get_extra_info('peername')
    log.info(f"New connection from {peername}")

    try:
        # Read the initial request line
        request_line = await client_reader.readline()
        if not request_line:
            return

        request_line = request_line.decode().strip()
        log.info(f"Request: {request_line}")

        if not request_line.startswith("CONNECT "):
            client_writer.write(b"HTTP/1.1 400 Bad Request\r\n\r\nOnly CONNECT supported\r\n")
            await client_writer.drain()
            return

        # Parse CONNECT target
        parts = request_line.split()
        if len(parts) < 2:
            client_writer.write(b"HTTP/1.1 400 Bad Request\r\n\r\nMalformed CONNECT\r\n")
            await client_writer.drain()
            return
        target = parts[1]  # host:port
        log.info(f"CONNECT to {target} via upstream proxy")

        # Drain the client's remaining request headers up to the blank line
        while True:
            header = await client_reader.readline()
            if not header or header in (b"\r\n", b"\n"):
                break

        # Connect to upstream proxy
        try:
            upstream_reader, upstream_writer = await asyncio.open_connection(UPSTREAM_HOST, UPSTREAM_PORT)
        except Exception as e:
            log.error(f"Failed to connect to upstream proxy: {e}")
            client_writer.write(b"HTTP/1.1 502 Bad Gateway\r\n\r\nUpstream proxy unreachable\r\n")
            await client_writer.drain()
            return

        # Send CONNECT to upstream with Proxy-Authorization
        connect_request = (
            f"CONNECT {target} HTTP/1.1\r\n"
            f"Host: {target}\r\n"
            f"Proxy-Authorization: Basic {AUTH_HEADER}\r\n"
            f"Proxy-Connection: Keep-Alive\r\n"
            f"\r\n"
        )
        upstream_writer.write(connect_request.encode())
        await upstream_writer.drain()

        # Read upstream response
        response_line = await upstream_reader.readline()
        if not response_line:
            log.error("No response from upstream proxy")
            client_writer.write(b"HTTP/1.1 502 Bad Gateway\r\n\r\nNo upstream response\r\n")
            await client_writer.drain()
            upstream_writer.close()
            return

        response_line = response_line.decode().strip()
        log.info(f"Upstream response: {response_line}")

        # Read remaining headers
        while True:
            header = await upstream_reader.readline()
            if not header or header in (b"\r\n", b"\n"):
                break

        if " 200 " not in response_line:
            log.error(f"Upstream proxy rejected CONNECT: {response_line}")
            client_writer.write(f"HTTP/1.1 502 Bad Gateway\r\n\r\nUpstream: {response_line}\r\n".encode())
            await client_writer.drain()
            upstream_writer.close()
            return

        # Success - send 200 to client
        client_writer.write(b"HTTP/1.1 200 Connection Established\r\n\r\n")
        await client_writer.drain()
        log.info(f"Tunnel established for {target}")

        # Start bidirectional piping
        await asyncio.gather(
            pipe(client_reader, upstream_writer),
            pipe(upstream_reader, client_writer),
        )

    except (ConnectionError, asyncio.CancelledError, OSError) as e:
        log.debug(f"Connection error: {e}")
    except Exception as e:
        log.exception(f"Unexpected error: {e}")
    finally:
        client_writer.close()
        try:
            await client_writer.wait_closed()
        except Exception:
            pass


async def main():
    server = await asyncio.start_server(handle_client, LISTEN_HOST, LISTEN_PORT)
    addrs = ', '.join(str(sock.getsockname()) for sock in server.sockets)
    log.info(f"HTTP CONNECT proxy listening on {addrs} -> {UPSTREAM_HOST}:{UPSTREAM_PORT}")

    async with server:
        await server.serve_forever()


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        log.info("Shutting down")